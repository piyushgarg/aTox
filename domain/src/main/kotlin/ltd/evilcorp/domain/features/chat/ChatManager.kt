// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.chat

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.core.di.IoDispatcher
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.core.network.MAX_MESSAGE_LENGTH
import ltd.evilcorp.domain.core.network.IToxMessenger

private fun String.chunked(chunkSizeInBytes: Int): MutableList<String> {
    val maxBytes = chunkSizeInBytes - 1
    val chunks = mutableListOf<String>()
    
    var currentChunk = StringBuilder()
    var currentBytes = 0
    
    var i = 0
    while (i < this.length) {
        val char = this[i]
        val isSurrogatePair = char.isHighSurrogate() && i + 1 < this.length && this[i + 1].isLowSurrogate()
        
        val symbolStr = if (isSurrogatePair) {
            this.substring(i, i + 2)
        } else {
            char.toString()
        }
        
        val symbolBytes = symbolStr.encodeToByteArray()
        val symbolSize = symbolBytes.size
        
        if (currentBytes + symbolSize > maxBytes) {
            if (currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toString())
                currentChunk = StringBuilder()
                currentBytes = 0
            }
            currentChunk.append(symbolStr)
            currentBytes = symbolSize
        } else {
            currentChunk.append(symbolStr)
            currentBytes += symbolSize
        }
        
        i += if (isSurrogatePair) 2 else 1
    }
    
    if (currentChunk.isNotEmpty()) {
        chunks.add(currentChunk.toString())
    }
    
    return chunks
}

@Singleton
class ChatManager @Inject constructor(
    private val scope: CoroutineScope,
    private val contactRepository: IContactRepository,
    private val messageRepository: IMessageRepository,
    private val tox: IToxMessenger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    var activeChat = ""
        set(value) {
            field = value
            if (value.isNotEmpty()) {
                scope.launch {
                    contactRepository.setHasUnreadMessages(value, false)
                }
            }
        }

    fun messagesFor(publicKey: PublicKey) = messageRepository.get(publicKey.string())

    suspend fun sendMessage(publicKey: PublicKey, message: String, type: MessageType = MessageType.Normal) = withContext(ioDispatcher) {
        if ((contactRepository.get(publicKey.string()).first()?.connectionStatus ?: ConnectionStatus.None) == ConnectionStatus.None) {
            queueMessage(publicKey, message, type)
            return@withContext
        }

        val msgs = message.chunked(MAX_MESSAGE_LENGTH)

        while (msgs.size > 1) {
            tox.sendMessage(publicKey, msgs.removeAt(0), type)
        }

        messageRepository.add(
            Message(
                publicKey.string(),
                message,
                Sender.Sent,
                type,
                tox.sendMessage(publicKey, msgs.first(), type),
            ),
        )
    }

    private suspend fun queueMessage(publicKey: PublicKey, message: String, type: MessageType) =
        messageRepository.add(Message(publicKey.string(), message, Sender.Sent, type, Int.MIN_VALUE))

    suspend fun resend(messages: List<Message>) = withContext(ioDispatcher) {
        for (message in messages) {
            val msgs = message.message.chunked(MAX_MESSAGE_LENGTH)

            while (msgs.size > 1) {
                tox.sendMessage(PublicKey(message.publicKey), msgs.removeAt(0), message.type)
            }

            messageRepository.setCorrelationId(
                message.id,
                tox.sendMessage(PublicKey(message.publicKey), msgs.first(), message.type),
            )
        }
    }

    suspend fun deleteMessage(id: Long) = withContext(ioDispatcher) {
        messageRepository.deleteMessage(id)
    }

    suspend fun clearHistory(publicKey: PublicKey) = withContext(ioDispatcher) {
        messageRepository.delete(publicKey.string())
        contactRepository.setLastMessage(publicKey.string(), 0)
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun setTyping(publicKey: PublicKey, typing: Boolean) = withContext(ioDispatcher) {
        tox.setTyping(publicKey, typing)
    }
}
