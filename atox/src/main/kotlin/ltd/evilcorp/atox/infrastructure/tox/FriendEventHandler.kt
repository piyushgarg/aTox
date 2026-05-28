package ltd.evilcorp.atox.infrastructure.tox

import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.features.contacts.repository.IFriendRequestRepository
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import ltd.evilcorp.domain.features.auth.repository.IUserRepository
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.core.model.FINGERPRINT_LEN
import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.features.transfer.sendAvatar
import ltd.evilcorp.domain.features.transfer.resetForContact
import ltd.evilcorp.domain.features.group.GroupManager
import ltd.evilcorp.domain.core.network.ITox

private const val MAX_ACTIVE_FRIEND_REQUESTS = 32
private const val TAG = "FriendEventHandler"
private const val SECONDS_TO_MS = 1000L

private fun String.fingerprint() = this.take(FINGERPRINT_LEN)

class FriendEventHandler @Inject constructor(
    private val scope: CoroutineScope,
    private val contactRepository: IContactRepository,
    private val friendRequestRepository: IFriendRequestRepository,
    private val messageRepository: IMessageRepository,
    private val userRepository: IUserRepository,
    private val chatManager: ChatManager,
    private val fileTransferManager: FileTransferManager,
    private val notificationHelper: NotificationHelper,
    private val systemSoundPlayer: SystemSoundPlayer,
    private val groupManager: GroupManager,
    private val tox: ITox,
    @Suppress("UNUSED_PARAMETER") private val settings: Settings,
) {
    private var maxFriendRequestsWarningActive = false

    private suspend fun tryGetContact(pk: String, tag: String) = contactRepository.get(pk).firstOrNull().let {
        if (it == null) {
            android.util.Log.e(TAG, "$tag -> unable to get contact for ${pk.fingerprint()}")
        }
        it
    }

    private fun notifyMessage(contact: Contact, message: String) =
        notificationHelper.showMessageNotification(contact, message, silent = tox.getStatus() == UserStatus.Busy)

    fun onFriendStatusMessage(publicKey: String, message: String) {
        scope.launch(Dispatchers.IO) {
            contactRepository.setStatusMessage(publicKey, message)
        }
    }

    fun onFriendReadReceipt(publicKey: String, messageId: Int) {
        scope.launch(Dispatchers.IO) {
            messageRepository.setReceipt(publicKey, messageId, Date().time)
        }
    }

    fun onFriendStatus(publicKey: String, status: UserStatus) {
        scope.launch(Dispatchers.IO) {
            contactRepository.setUserStatus(publicKey, status)
        }
    }

    fun onFriendConnectionStatus(publicKey: String, status: ConnectionStatus) {
        scope.launch(Dispatchers.IO) {
            contactRepository.setConnectionStatus(publicKey, status)
            if (status != ConnectionStatus.None) {
                groupManager.reconnectAll()
                fileTransferManager.sendAvatar(publicKey)
                val pending = messageRepository.getPending(publicKey)
                if (pending.isNotEmpty()) {
                    chatManager.resend(pending)
                }
            } else {
                fileTransferManager.resetForContact(publicKey)
                val lastOnline = try {
                    tox.friendGetLastOnline(ltd.evilcorp.domain.core.model.PublicKey(publicKey))
                } catch (e: Exception) {
                    0L
                }
                if (lastOnline > 0L) {
                    contactRepository.setLastOnline(publicKey, lastOnline * SECONDS_TO_MS)
                }
            }
        }
    }

    fun onFriendRequest(publicKey: String, message: String) {
        if (friendRequestRepository.count() > MAX_ACTIVE_FRIEND_REQUESTS) {
            if (!maxFriendRequestsWarningActive) {
                android.util.Log.w(TAG, "Ignoring friend requests w/ $MAX_ACTIVE_FRIEND_REQUESTS already active")
                maxFriendRequestsWarningActive = true
            }
            return
        }

        maxFriendRequestsWarningActive = false
        val request = FriendRequest(publicKey, message)
        friendRequestRepository.add(request)
        notificationHelper.showFriendRequestNotification(request, silent = tox.getStatus() == UserStatus.Busy)
    }

    fun onFriendMessage(publicKey: String, type: ToxMessageType, message: String) {
        scope.launch(Dispatchers.IO) {
            messageRepository.add(
                Message(publicKey, message, Sender.Received, type.toMessageType(), Int.MIN_VALUE, Date().time),
            )

            if (chatManager.activeChat != publicKey) {
                systemSoundPlayer.playNotificationSound(settings.notificationSoundUri, settings.notificationSoundVolume)
                val contact = tryGetContact(publicKey, "Message") ?: Contact(publicKey)
                notifyMessage(contact, message)
                contactRepository.setHasUnreadMessages(publicKey, true)
            } else {
                systemSoundPlayer.playNotificationSound(settings.activeChatSoundUri, settings.activeChatSoundVolume)
            }
        }
    }

    fun onFriendName(publicKey: String, newName: String) {
        scope.launch(Dispatchers.IO) {
            contactRepository.setName(publicKey, newName)
        }
    }

    fun onSelfConnectionStatus(status: ConnectionStatus) {
        userRepository.updateConnection(tox.publicKey.string(), status)
        if (status != ConnectionStatus.None) {
            scope.launch {
                groupManager.reconnectAll()
            }
        }
    }

    fun onFriendTyping(publicKey: String, isTyping: Boolean) {
        scope.launch(Dispatchers.IO) {
            contactRepository.setTyping(publicKey, isTyping)
        }
    }
}
