package ltd.evilcorp.atox.infrastructure.tox

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import ltd.evilcorp.domain.features.group.GroupEventBus
import ltd.evilcorp.domain.features.group.GroupDomainEvent
import ltd.evilcorp.domain.features.group.GroupManager
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.bytesToHex

private const val TAG = "GroupSoundPlayer"

/**
 * Reactive sound player component for playing alert tones during active group chats or invites.
 * Listens to the shared [GroupEventBus] flow.
 */
@Singleton
class GroupSoundPlayer @Inject constructor(
    private val scope: CoroutineScope,
    private val groupEventBus: GroupEventBus,
    private val systemSoundPlayer: SystemSoundPlayer,
    private val groupRepository: IGroupRepository,
    private val groupManager: GroupManager,
    private val settings: Settings,
    private val tox: ITox,
) {
    init {
        scope.launch {
            groupEventBus.events.collect { event ->
                try {
                    processEvent(event)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in GroupSoundPlayer: $event", e)
                }
            }
        }
    }

    private suspend fun processEvent(event: GroupDomainEvent) {
        when (event) {
            is GroupDomainEvent.GroupInvite -> {
                systemSoundPlayer.playNotificationSound(settings.notificationSoundUri, settings.notificationSoundVolume)
            }
            is GroupDomainEvent.GroupMessage -> handleGroupMessage(event)
            else -> {}
        }
    }

    private suspend fun handleGroupMessage(event: GroupDomainEvent.GroupMessage) {
        val chatIdBytes = tox.groupGetChatId(event.groupNo)
        val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: return

        val group = groupRepository.get(chatId).firstOrNull() ?: return
        val isOurPeer = event.peerId == group.selfPeerId

        if (!isOurPeer && groupManager.activeGroup != chatId) {
            systemSoundPlayer.playNotificationSound(settings.notificationSoundUri, settings.notificationSoundVolume)
        } else {
            systemSoundPlayer.playNotificationSound(settings.activeChatSoundUri, settings.activeChatSoundVolume)
        }
    }
}
