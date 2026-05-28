package ltd.evilcorp.atox.infrastructure.tox

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.group.GroupEventBus
import ltd.evilcorp.domain.features.group.GroupDomainEvent

private const val TAG = "GroupEventProcessor"

/**
 * Pure JNI-to-Domain mapper and event processor.
 * Collects low-level JNI events from [GroupEventHandler] and dispatches mapped domain-level events to the [GroupEventBus].
 * Eagerly injects independent reactive handlers to maintain active event loop lifecycles on app start.
 */
@Singleton
@Suppress("UnusedPrivateProperty")
class GroupEventProcessor @Inject constructor(
    private val scope: CoroutineScope,
    private val groupEventHandler: GroupEventHandler,
    private val groupEventBus: GroupEventBus,
    // Eagerly inject independent reactive handlers to trigger Hilt initialization
    private val databaseUpdater: GroupDatabaseUpdater,
    private val notificationDispatcher: GroupNotificationDispatcher,
    private val soundPlayer: GroupSoundPlayer,
) {
    init {
        scope.launch {
            groupEventHandler.groupEvents.collect { event ->
                try {
                    val domainEvent = mapToDomain(event)
                    if (domainEvent != null) {
                        groupEventBus.emit(domainEvent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping JNI group event: $event", e)
                }
            }
        }
    }

    private fun mapToDomain(event: GroupJniEvent): GroupDomainEvent? {
        return when (event) {
            is GroupJniEvent.GroupInvite -> GroupDomainEvent.GroupInvite(event.friendNo, event.inviteData, event.groupName)
            is GroupJniEvent.GroupMessage -> GroupDomainEvent.GroupMessage(event.groupNo, event.peerId, event.type, event.message, event.messageId)
            is GroupJniEvent.GroupPeerJoin -> GroupDomainEvent.GroupPeerJoin(event.groupNo, event.peerId)
            is GroupJniEvent.GroupPeerExit -> GroupDomainEvent.GroupPeerExit(event.groupNo, event.peerId, event.exitType)
            is GroupJniEvent.GroupTopic -> GroupDomainEvent.GroupTopic(event.groupNo, event.peerId, event.topic)
            is GroupJniEvent.GroupPeerName -> GroupDomainEvent.GroupPeerName(event.groupNo, event.peerId, event.name)
            is GroupJniEvent.GroupPassword -> GroupDomainEvent.GroupPassword(event.groupNo, event.password)
            is GroupJniEvent.GroupPeerStatus -> GroupDomainEvent.GroupPeerStatus(event.groupNo, event.peerId, event.status.ordinal)
            is GroupJniEvent.GroupPrivacyStateChanged -> GroupDomainEvent.GroupPrivacyStateChanged(event.groupNo, event.privacyState)
            is GroupJniEvent.GroupVoiceState -> GroupDomainEvent.GroupVoiceState(event.groupNo, event.voiceState.ordinal)
            is GroupJniEvent.GroupTopicLock -> GroupDomainEvent.GroupTopicLock(event.groupNo, event.topicLock.ordinal)
            is GroupJniEvent.GroupPeerLimit -> GroupDomainEvent.GroupPeerLimit(event.groupNo, event.peerLimit)
            is GroupJniEvent.GroupPrivateMessage -> GroupDomainEvent.GroupPrivateMessage(event.groupNo, event.peerId, event.type, event.message)
            is GroupJniEvent.GroupConnected -> GroupDomainEvent.GroupConnected(event.groupNo)
            is GroupJniEvent.GroupJoinFail -> GroupDomainEvent.GroupJoinFail(event.groupNo, event.joinFail)
            is GroupJniEvent.GroupModeration -> GroupDomainEvent.GroupModeration(event.groupNo, event.sourcePeerId, event.targetPeerId, event.modEvent)
        }
    }
}
