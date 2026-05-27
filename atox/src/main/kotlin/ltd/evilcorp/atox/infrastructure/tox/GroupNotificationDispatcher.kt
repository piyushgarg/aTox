package ltd.evilcorp.atox.infrastructure.tox

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.domain.repository.IGroupRepository
import ltd.evilcorp.domain.feature.GroupEventBus
import ltd.evilcorp.domain.feature.GroupDomainEvent
import ltd.evilcorp.domain.feature.GroupManager
import ltd.evilcorp.domain.tox.ITox
import ltd.evilcorp.domain.tox.bytesToHex

private const val TAG = "GroupNotificationDispatcher"

/**
 * Reactive notification dispatcher for group invite and messaging events.
 * Listens to the shared [GroupEventBus] flow.
 */
@Singleton
class GroupNotificationDispatcher @Inject constructor(
    private val scope: CoroutineScope,
    private val groupEventBus: GroupEventBus,
    private val notificationHelper: NotificationHelper,
    private val groupRepository: IGroupRepository,
    private val groupManager: GroupManager,
    private val tox: ITox,
) {
    init {
        scope.launch {
            groupEventBus.events.collect { event ->
                try {
                    processEvent(event)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in GroupNotificationDispatcher: $event", e)
                }
            }
        }
    }

    private suspend fun processEvent(event: GroupDomainEvent) {
        when (event) {
            is GroupDomainEvent.GroupInvite -> handleGroupInvite(event)
            is GroupDomainEvent.GroupMessage -> handleGroupMessage(event)
            else -> {}
        }
    }

    private suspend fun handleGroupInvite(event: GroupDomainEvent.GroupInvite) {
        val friendPkObject = tox.getFriendPublicKey(event.friendNo)
        if (friendPkObject != null) {
            val friendPk = friendPkObject.string().lowercase()
            notificationHelper.showGroupInviteNotification(friendPk, event.groupName)
        }
    }

    private suspend fun handleGroupMessage(event: GroupDomainEvent.GroupMessage) {
        val chatIdBytes = tox.groupGetChatId(event.groupNo)
        val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: return

        val group = groupRepository.get(chatId).firstOrNull() ?: return
        val isOurPeer = event.peerId == group.selfPeerId

        if (!isOurPeer && groupManager.activeGroup != chatId) {
            val peerNameBytes = tox.groupPeerGetName(event.groupNo, event.peerId)
            val peerName = peerNameBytes?.decodeToString() ?: "Unknown"
            notificationHelper.showGroupMessageNotification(group.name, peerName, event.message)
        }
    }
}
