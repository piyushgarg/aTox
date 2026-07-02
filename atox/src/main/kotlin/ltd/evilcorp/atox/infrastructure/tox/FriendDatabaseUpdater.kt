// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.infrastructure.tox

import android.util.Log
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.contacts.IToxFriendEventBus
import ltd.evilcorp.domain.features.contacts.model.ToxFriendEvent
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.features.contacts.repository.IFriendRequestRepository
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import ltd.evilcorp.domain.features.auth.repository.IUserRepository
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.features.chat.ChatManager

private const val MAX_ACTIVE_FRIEND_REQUESTS = 32
private const val MESSAGE_BATCH_SIZE = 10
private const val TAG = "FriendDatabaseUpdater"
private const val SECONDS_TO_MS = 1000L

@Singleton
class FriendDatabaseUpdater @Inject constructor(
    private val scope: CoroutineScope,
    private val eventBus: IToxFriendEventBus,
    private val contactRepository: IContactRepository,
    private val friendRequestRepository: IFriendRequestRepository,
    private val messageRepository: IMessageRepository,
    private val userRepository: IUserRepository,
    private val chatManager: ChatManager,
    private val tox: ITox,
) {
    init {
        scope.launch(Dispatchers.IO) {
            val messageBuffer = mutableListOf<Message>()
            eventBus.events.collect { event ->
                try {
                    if (event is ToxFriendEvent.FriendMessage) {
                        val msgType = event.type.toMessageType()
                        messageBuffer.add(
                            Message(
                                event.publicKey,
                                event.message,
                                Sender.Received,
                                msgType,
                                Int.MIN_VALUE,
                                Date().time
                            )
                        )
                        if (messageBuffer.size >= MESSAGE_BATCH_SIZE) {
                            flushMessageBuffer(messageBuffer)
                        }
                    } else {
                        flushMessageBuffer(messageBuffer)
                        processEvent(event)
                    }
                } catch (e: Exception) {
                    flushMessageBuffer(messageBuffer)
                    Log.e(TAG, "Error processing event: $event", e)
                }
            }
        }
    }

    private suspend fun flushMessageBuffer(buffer: MutableList<Message>) {
        if (buffer.isEmpty()) return
        val batch = buffer.toList()
        buffer.clear()
        messageRepository.addAll(batch)
        val unreadKeys = batch.map { it.publicKey }.distinct()
        for (key in unreadKeys) {
            if (chatManager.activeChat != key) {
                contactRepository.setHasUnreadMessages(key, true)
            }
        }
    }

    private suspend fun processEvent(event: ToxFriendEvent) {
        when (event) {
            is ToxFriendEvent.FriendStatusMessage -> {
                contactRepository.setStatusMessage(event.publicKey, event.message)
            }
            is ToxFriendEvent.FriendReadReceipt -> {
                messageRepository.setReceipt(event.publicKey, event.messageId, Date().time)
            }
            is ToxFriendEvent.FriendStatus -> {
                contactRepository.setUserStatus(event.publicKey, event.status)
            }
            is ToxFriendEvent.FriendConnectionStatus -> {
                contactRepository.setConnectionStatus(event.publicKey, event.status)
                if (event.status == ConnectionStatus.None) {
                    val lastOnline = try {
                        tox.friendGetLastOnline(ltd.evilcorp.domain.core.model.PublicKey(event.publicKey))
                    } catch (e: Exception) {
                        0L
                    }
                    if (lastOnline > 0L) {
                        contactRepository.setLastOnline(event.publicKey, lastOnline * SECONDS_TO_MS)
                    }
                }
            }
            is ToxFriendEvent.FriendRequest -> {
                if (friendRequestRepository.count() <= MAX_ACTIVE_FRIEND_REQUESTS) {
                    val request = FriendRequest(event.publicKey, event.message)
                    friendRequestRepository.add(request)
                }
            }
            is ToxFriendEvent.FriendMessage -> {
                // Handled inline via messageBuffer in init
            }
            is ToxFriendEvent.FriendName -> {
                contactRepository.setName(event.publicKey, event.newName)
            }
            is ToxFriendEvent.SelfConnectionStatus -> {
                userRepository.updateConnection(tox.publicKey.string(), event.status)
            }
            is ToxFriendEvent.FriendTyping -> {
                contactRepository.setTyping(event.publicKey, event.isTyping)
            }
        }
    }
}
