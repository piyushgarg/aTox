package ltd.evilcorp.atox.infrastructure.tox

import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.core.repository.FriendRequestRepository
import ltd.evilcorp.core.repository.MessageRepository
import ltd.evilcorp.core.repository.UserRepository
import ltd.evilcorp.domain.model.ConnectionStatus
import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.FriendRequest
import ltd.evilcorp.domain.model.Message
import ltd.evilcorp.domain.model.Sender
import ltd.evilcorp.domain.model.UserStatus
import ltd.evilcorp.domain.model.FINGERPRINT_LEN
import ltd.evilcorp.domain.tox.enums.ToxMessageType
import ltd.evilcorp.domain.feature.ChatManager
import ltd.evilcorp.domain.feature.FileTransferManager
import ltd.evilcorp.domain.feature.GroupManager
import ltd.evilcorp.domain.tox.ITox

private const val MAX_ACTIVE_FRIEND_REQUESTS = 32
private const val TAG = "FriendEventHandler"

private fun String.fingerprint() = this.take(FINGERPRINT_LEN)

class FriendEventHandler @Inject constructor(
    private val scope: CoroutineScope,
    private val contactRepository: ContactRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
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
        contactRepository.setStatusMessage(publicKey, message)
    }

    fun onFriendReadReceipt(publicKey: String, messageId: Int) {
        messageRepository.setReceipt(publicKey, messageId, Date().time)
    }

    fun onFriendStatus(publicKey: String, status: UserStatus) {
        contactRepository.setUserStatus(publicKey, status)
    }

    fun onFriendConnectionStatus(publicKey: String, status: ConnectionStatus) {
        contactRepository.setConnectionStatus(publicKey, status)
        if (status != ConnectionStatus.None) {
            groupManager.reconnectAll()
            scope.launch {
                fileTransferManager.sendAvatar(publicKey)
            }
            scope.launch {
                val pending = messageRepository.getPending(publicKey)
                if (pending.isNotEmpty()) {
                    chatManager.resend(pending)
                }
            }
        } else {
            fileTransferManager.resetForContact(publicKey)
            val lastOnline = try {
                tox.friendGetLastOnline(ltd.evilcorp.domain.model.PublicKey(publicKey))
            } catch (e: Exception) {
                0L
            }
            if (lastOnline > 0L) {
                contactRepository.setLastOnline(publicKey, lastOnline * 1000)
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
        messageRepository.add(
            Message(publicKey, message, Sender.Received, type.toMessageType(), Int.MIN_VALUE, Date().time),
        )

        if (chatManager.activeChat != publicKey) {
            systemSoundPlayer.playNotificationSound(settings.notificationSoundUri, settings.notificationSoundVolume)
            scope.launch {
                val contact = tryGetContact(publicKey, "Message") ?: Contact(publicKey)
                notifyMessage(contact, message)
            }
            contactRepository.setHasUnreadMessages(publicKey, true)
        } else {
            systemSoundPlayer.playNotificationSound(settings.activeChatSoundUri, settings.activeChatSoundVolume)
        }
    }

    fun onFriendName(publicKey: String, newName: String) {
        contactRepository.setName(publicKey, newName)
    }

    fun onSelfConnectionStatus(status: ConnectionStatus) {
        userRepository.updateConnection(tox.publicKey.string(), status)
        if (status != ConnectionStatus.None) {
            groupManager.reconnectAll()
        }
    }

    fun onFriendTyping(publicKey: String, isTyping: Boolean) {
        contactRepository.setTyping(publicKey, isTyping)
    }
}
