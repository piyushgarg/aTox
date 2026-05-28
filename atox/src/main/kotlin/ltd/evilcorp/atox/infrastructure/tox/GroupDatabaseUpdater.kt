package ltd.evilcorp.atox.infrastructure.tox

import android.util.Log
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import ltd.evilcorp.domain.features.transfer.repository.IFileTransferRepository
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.core.network.enums.ToxGroupExitType
import ltd.evilcorp.domain.core.network.enums.ToxGroupJoinFail
import ltd.evilcorp.domain.core.network.enums.ToxGroupModEvent
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxGroupRole
import ltd.evilcorp.domain.core.network.enums.ToxUserStatus
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.GroupInvite
import ltd.evilcorp.domain.features.group.GroupManager
import ltd.evilcorp.domain.features.group.GroupEventBus
import ltd.evilcorp.domain.features.group.GroupDomainEvent
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.bytesToHex

private const val TAG = "GroupDatabaseUpdater"

/**
 * Reactive Room database updater for all group NGC message/peer events.
 * Listens to the clean domain-level [GroupEventBus] events asynchronously.
 */
@Singleton
class GroupDatabaseUpdater @Inject constructor(
    private val scope: CoroutineScope,
    internal val groupRepository: IGroupRepository,
    private val messageRepository: IMessageRepository,
    private val fileTransferRepository: IFileTransferRepository,
    internal val groupManager: GroupManager,
    private val groupEventBus: GroupEventBus,
    internal val tox: ITox,
) {
    init {
        scope.launch {
            groupEventBus.events.collect { event ->
                try {
                    withContext(Dispatchers.IO) {
                        processEvent(event)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in GroupDatabaseUpdater: $event", e)
                }
            }
        }
    }

    private suspend fun processEvent(event: GroupDomainEvent) {
        when (event) {
            is GroupDomainEvent.GroupInvite -> handleGroupInvite(event, messageRepository)
            is GroupDomainEvent.GroupMessage -> handleGroupMessage(event, fileTransferRepository)
            is GroupDomainEvent.GroupPeerJoin -> handleGroupPeerJoin(event)
            is GroupDomainEvent.GroupPeerExit -> handleGroupPeerExit(event)
            is GroupDomainEvent.GroupTopic -> handleGroupTopic(event)
            is GroupDomainEvent.GroupPeerName -> handleGroupPeerName(event)
            is GroupDomainEvent.GroupPassword -> handleGroupPassword(event)
            is GroupDomainEvent.GroupPeerStatus -> handleGroupPeerStatus(event)
            is GroupDomainEvent.GroupPrivacyStateChanged -> handleGroupPrivacyState(event)
            is GroupDomainEvent.GroupVoiceState -> handleGroupVoiceState(event)
            is GroupDomainEvent.GroupTopicLock -> handleGroupTopicLock(event)
            is GroupDomainEvent.GroupPeerLimit -> handleGroupPeerLimit(event)
            is GroupDomainEvent.GroupPrivateMessage -> handleGroupPrivateMessage(event)
            is GroupDomainEvent.GroupConnected -> handleGroupConnected(event)
            is GroupDomainEvent.GroupJoinFail -> handleGroupJoinFail(event)
            is GroupDomainEvent.GroupModeration -> handleGroupModeration(event)
        }
    }



    private suspend fun handleGroupTopic(event: GroupDomainEvent.GroupTopic) {
        val chatIdBytes = tox.groupGetChatId(event.groupNo)
        val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: return
        checkAndMigrateTemporaryGroup(event.groupNo, chatId)
        groupRepository.setTopic(chatId, event.topic)
        Log.i(TAG, "Group topic changed in $chatId")
    }

    private suspend fun handleGroupPeerName(event: GroupDomainEvent.GroupPeerName) {
        val chatIdBytes = tox.groupGetChatId(event.groupNo)
        val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: return
        checkAndMigrateTemporaryGroup(event.groupNo, chatId)
        groupRepository.setPeerName(chatId, event.peerId, event.name)
    }

    private suspend fun handleGroupPassword(event: GroupDomainEvent.GroupPassword) {
        val chatIdBytes = tox.groupGetChatId(event.groupNo)
        val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: return
        checkAndMigrateTemporaryGroup(event.groupNo, chatId)
        groupRepository.setPasswordProtected(chatId, event.password.isNotEmpty())
    }

    private suspend fun handleGroupPeerStatus(event: GroupDomainEvent.GroupPeerStatus) {
        val chatIdBytes = tox.groupGetChatId(event.groupNo)
        val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: return
        checkAndMigrateTemporaryGroup(event.groupNo, chatId)
        val userStatus = when (event.status) {
            ToxUserStatus.NONE.ordinal -> ltd.evilcorp.domain.features.contacts.model.UserStatus.None
            ToxUserStatus.AWAY.ordinal -> ltd.evilcorp.domain.features.contacts.model.UserStatus.Away
            ToxUserStatus.BUSY.ordinal -> ltd.evilcorp.domain.features.contacts.model.UserStatus.Busy
            else -> ltd.evilcorp.domain.features.contacts.model.UserStatus.None
        }
        groupRepository.setPeerStatus(chatId, event.peerId, userStatus)
    }

    private suspend fun handleGroupPrivacyState(event: GroupDomainEvent.GroupPrivacyStateChanged) {
        val chatIdBytes = tox.groupGetChatId(event.groupNo)
        val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: return
        checkAndMigrateTemporaryGroup(event.groupNo, chatId)
        val state = when (event.privacyState) {
            ToxGroupPrivacyState.PUBLIC -> ltd.evilcorp.domain.features.group.model.GroupPrivacyState.Public
            ToxGroupPrivacyState.PRIVATE -> ltd.evilcorp.domain.features.group.model.GroupPrivacyState.Private
        }
        groupRepository.setPrivacyState(chatId, state)
    }

    private fun handleGroupVoiceState(event: GroupDomainEvent.GroupVoiceState) {
        Log.i(TAG, "Group voice state changed in groupNo=${event.groupNo}, state=${event.voiceState}")
    }

    private fun handleGroupTopicLock(event: GroupDomainEvent.GroupTopicLock) {
        Log.i(TAG, "Group topic lock changed in groupNo=${event.groupNo}, lock=${event.topicLock}")
    }

    private fun handleGroupPeerLimit(event: GroupDomainEvent.GroupPeerLimit) {
        Log.i(TAG, "Group peer limit changed in groupNo=${event.groupNo}, limit=${event.peerLimit}")
    }

    private suspend fun handleGroupPrivateMessage(event: GroupDomainEvent.GroupPrivateMessage) {
        val chatIdBytes = tox.groupGetChatId(event.groupNo)
        val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: return
        checkAndMigrateTemporaryGroup(event.groupNo, chatId)
        val peerNameBytes = tox.groupPeerGetName(event.groupNo, event.peerId)
        val peerName = peerNameBytes?.decodeToString() ?: "Unknown"
        Log.i(TAG, "Private message in group $chatId from $peerName: ${event.message}")
    }

    private suspend fun handleGroupConnected(event: GroupDomainEvent.GroupConnected) {
        val chatIdBytes = tox.groupGetChatId(event.groupNo)
        val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: return
        checkAndMigrateTemporaryGroup(event.groupNo, chatId)
        groupRepository.setConnected(chatId, true)
        groupRepository.setGroupNumber(chatId, event.groupNo)
        groupManager.setConnectionStatus(chatId, GroupConnectionStatus.Connected)
        groupManager.cancelReconnect(chatId)

        checkAndUpdateGroupMetadata(event.groupNo, chatId)
        groupManager.resendPendingMessages(chatId)
        Log.i(TAG, "Connected to group $chatId")
    }

    private suspend fun handleGroupJoinFail(event: GroupDomainEvent.GroupJoinFail) {
        Log.e(TAG, "Failed to join groupNo=${event.groupNo}, reason=${event.joinFail}")
        val chatId = groupRepository.findChatIdByGroupNumber(event.groupNo)
        if (chatId != null) {
            groupRepository.setConnected(chatId, false)
            groupManager.setConnectionStatus(chatId, GroupConnectionStatus.Disconnected)
        }
    }

    private suspend fun handleGroupModeration(event: GroupDomainEvent.GroupModeration) {
        val chatIdBytes = tox.groupGetChatId(event.groupNo)
        val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: return

        checkAndMigrateTemporaryGroup(event.groupNo, chatId)
        val sourceNameBytes = tox.groupPeerGetName(event.groupNo, event.sourcePeerId)
        val sourceName = sourceNameBytes?.decodeToString() ?: "Unknown"

        Log.i(TAG, "Moderation in $chatId: $sourceName performed ${event.modEvent} on peer ${event.targetPeerId}")

        if (event.modEvent == ToxGroupModEvent.KICK || event.modEvent == ToxGroupModEvent.MODERATOR || event.modEvent == ToxGroupModEvent.USER) {
            val role = when (event.modEvent) {
                ToxGroupModEvent.MODERATOR -> ToxGroupRole.MODERATOR.name
                ToxGroupModEvent.USER -> ToxGroupRole.USER.name
                else -> null
            }
            if (role != null) {
                groupRepository.setPeerRole(chatId, event.targetPeerId, role)
            }
        }
    }
}
