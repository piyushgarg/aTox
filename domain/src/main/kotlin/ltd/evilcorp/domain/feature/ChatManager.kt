// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.feature

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.repository.IContactRepository
import ltd.evilcorp.domain.repository.IMessageRepository
import ltd.evilcorp.domain.model.ConnectionStatus
import ltd.evilcorp.domain.model.Message
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.domain.model.PublicKey
import ltd.evilcorp.domain.model.Sender
import ltd.evilcorp.domain.tox.MAX_MESSAGE_LENGTH
import ltd.evilcorp.domain.tox.ITox

private fun String.chunked(chunkSizeInBytes: Int): MutableList<String> {
    val maxBytes = chunkSizeInBytes - 1
    val chunks = mutableListOf<String>()
    
    var currentChunk = java.lang.StringBuilder()
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
                currentChunk = java.lang.StringBuilder()
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
    private val tox: ITox,
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

    fun sendMessage(publicKey: PublicKey, message: String, type: MessageType = MessageType.Normal) = scope.launch {
        if ((contactRepository.get(publicKey.string()).first()?.connectionStatus ?: ConnectionStatus.None) == ConnectionStatus.None) {
            queueMessage(publicKey, message, type)
            return@launch
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

    private fun queueMessage(publicKey: PublicKey, message: String, type: MessageType) =
        messageRepository.add(Message(publicKey.string(), message, Sender.Sent, type, Int.MIN_VALUE))

    fun resend(messages: List<Message>) = scope.launch {
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

    fun deleteMessage(id: Long) = scope.launch {
        messageRepository.deleteMessage(id)
    }

    fun clearHistory(publicKey: PublicKey) = scope.launch {
        messageRepository.delete(publicKey.string())
        contactRepository.setLastMessage(publicKey.string(), 0)
    }

    fun setTyping(publicKey: PublicKey, typing: Boolean) = scope.launch {
        tox.setTyping(publicKey, typing)
    }
}
