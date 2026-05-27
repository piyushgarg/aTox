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
import ltd.evilcorp.domain.repository.IGroupRepository
import ltd.evilcorp.domain.repository.IMessageRepository
import ltd.evilcorp.domain.repository.IFileTransferRepository
import ltd.evilcorp.domain.model.Message
import ltd.evilcorp.domain.model.Group
import ltd.evilcorp.domain.model.GroupMessage
import ltd.evilcorp.domain.model.GroupPeer
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.domain.model.Sender
import ltd.evilcorp.domain.model.FileTransfer
import ltd.evilcorp.domain.tox.enums.ToxGroupExitType
import ltd.evilcorp.domain.tox.enums.ToxGroupJoinFail
import ltd.evilcorp.domain.tox.enums.ToxGroupModEvent
import ltd.evilcorp.domain.tox.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.tox.enums.ToxGroupRole
import ltd.evilcorp.domain.tox.enums.ToxUserStatus
import ltd.evilcorp.domain.feature.GroupConnectionStatus
import ltd.evilcorp.domain.feature.GroupInvite
import ltd.evilcorp.domain.feature.GroupManager
import ltd.evilcorp.domain.feature.GroupEventBus
import ltd.evilcorp.domain.feature.GroupDomainEvent
import ltd.evilcorp.domain.tox.ITox
import ltd.evilcorp.domain.tox.bytesToHex

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
            is GroupDomainEvent.GroupInvite -> handleGroupInvite(event)
            is GroupDomainEvent.GroupMessage -> handleGroupMessage(event)
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

    private suspend fun handleGroupInvite(event: GroupDomainEvent.GroupInvite) {
        try {
            val friendPkObject = tox.getFriendPublicKey(event.friendNo)
            if (friendPkObject != null) {
                val friendPk = friendPkObject.string().lowercase()
                val inviteDataHex = event.inviteData.bytesToHex().lowercase()
                val inviteText = "[GROUP_INVITE:${event.groupName}|$inviteDataHex]"
                
                val alreadyExists = messageRepository.exists(friendPk, inviteText)
                if (!alreadyExists) {
                    val msg = Message(
                        publicKey = friendPk,
                        message = inviteText,
                        sender = Sender.Received,
                        type = MessageType.Normal,
                        correlationId = 0,
                        timestamp = Date().time
                    )
                    messageRepository.add(msg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting group invite to chat", e)
        }
        val invite = GroupInvite(friendNo = event.friendNo, inviteData = event.inviteData, groupName = event.groupName)
        groupManager.setPendingInvite(invite)
    }

    private suspend fun handleGroupMessage(event: GroupDomainEvent.GroupMessage) {
        val chatIdBytes = tox.groupGetChatId(event.groupNo)
        val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: return

        checkAndMigrateTemporaryGroup(event.groupNo, chatId)
        checkAndUpdateGroupMetadata(event.groupNo, chatId)

        val peerNameBytes = tox.groupPeerGetName(event.groupNo, event.peerId)
        val peerName = peerNameBytes?.decodeToString() ?: "Unknown"

        val group = groupRepository.get(chatId).firstOrNull()
        if (group == null) {
            Log.e(TAG, "Group not found for chatId=$chatId")
            return
        }

        val isOurPeer = event.peerId == group.selfPeerId

        if (groupRepository.existsByCorrelationId(chatId, event.messageId)) {
            return
        }

        val isFile = event.message.startsWith("[FILE:")
        val isVoice = event.message.startsWith("[VOICE:")
        val msgType = if (isFile || isVoice) MessageType.FileTransfer else event.type.toMessageType()
        var corrId = event.messageId

        if (isFile) {
            parseAndRegisterGroupFile(chatId, event.message)?.let {
                corrId = it
            }
        } else if (isVoice) {
            parseAndRegisterGroupVoice(chatId, event.message)?.let {
                corrId = it
            }
        }

        val groupMsg = GroupMessage(
            groupChatId = chatId,
            peerId = event.peerId,
            senderName = peerName,
            message = event.message,
            sender = if (isOurPeer) Sender.Sent else Sender.Received,
            type = msgType,
            correlationId = corrId,
            timestamp = Date().time,
        )
        groupRepository.addMessage(groupMsg)

        if (!isOurPeer && groupManager.activeGroup != chatId) {
            groupRepository.setHasUnreadMessages(chatId, true)
        }
    }

    private suspend fun parseAndRegisterGroupFile(chatId: String, message: String): Int? {
        try {
            val parts = message.removePrefix("[FILE:").removeSuffix("]").split("|")
            if (parts.size >= 3) {
                val fileName = parts[0]
                val fileSize = parts[1].toLong()
                val originalCorrId = parts[2].toInt()

                val ft = FileTransfer(
                    publicKey = chatId,
                    fileNumber = originalCorrId,
                    fileKind = ltd.evilcorp.domain.model.FileKind.Data.ordinal,
                    fileSize = fileSize,
                    fileName = fileName,
                    outgoing = false,
                    progress = ltd.evilcorp.domain.model.FT_NOT_STARTED,
                    destination = "",
                )
                fileTransferRepository.add(ft)
                return originalCorrId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse incoming group file message: $message", e)
        }
        return null
    }

    private suspend fun parseAndRegisterGroupVoice(chatId: String, message: String): Int? {
        try {
            val parts = message.removePrefix("[VOICE:").removeSuffix("]").split("|")
            if (parts.size >= 2) {
                val duration = parts[0].toInt()
                val originalCorrId = parts[1].toInt()

                val ft = FileTransfer(
                    publicKey = chatId,
                    fileNumber = originalCorrId,
                    fileKind = ltd.evilcorp.domain.model.FileKind.Data.ordinal,
                    fileSize = duration * 1000L,
                    fileName = "voice_message_${originalCorrId}.m4a",
                    outgoing = false,
                    progress = ltd.evilcorp.domain.model.FT_NOT_STARTED,
                    destination = "",
                )
                fileTransferRepository.add(ft)
                return originalCorrId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse incoming group voice message: $message", e)
        }
        return null
    }

    private suspend fun handleGroupPeerJoin(event: GroupDomainEvent.GroupPeerJoin) {
        val chatIdBytes = tox.groupGetChatId(event.groupNo)
        val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: return

        checkAndMigrateTemporaryGroup(event.groupNo, chatId)
        checkAndUpdateGroupMetadata(event.groupNo, chatId)

        val group = groupRepository.get(chatId).firstOrNull() ?: return

        val peerNameBytes = tox.groupPeerGetName(event.groupNo, event.peerId)
        val peerName = peerNameBytes?.decodeToString() ?: "Unknown"

        if (event.peerId != group.selfPeerId) {
            val peerKeyBytes = tox.groupPeerGetPublicKey(event.groupNo, event.peerId)
            val peerKey = peerKeyBytes?.bytesToHex()?.uppercase() ?: ""

            val alreadyExistsByPubKey = peerKey.isNotEmpty() && groupRepository.peerExistsByPublicKey(chatId, peerKey)

            val isNewPeer = if (alreadyExistsByPubKey) {
                groupRepository.deletePeerByPublicKey(chatId, peerKey)
                false
            } else {
                true
            }

            val peer = GroupPeer(
                groupChatId = chatId,
                peerId = event.peerId,
                name = peerName,
                publicKey = peerKey,
            )
            groupRepository.addPeer(peer)

            if (isNewPeer) {
                val msg = GroupMessage(
                    groupChatId = chatId,
                    peerId = event.peerId,
                    senderName = peerName,
                    message = "[SYSTEM_EVENT:PEER_JOINED|$peerName]",
                    sender = Sender.Received,
                    type = MessageType.GroupEvent,
                    correlationId = 0,
                    timestamp = Date().time,
                )
                groupRepository.addMessage(msg)
            }
        }

        val count = groupRepository.peerCountDirect(chatId)
        groupRepository.setPeerCount(chatId, count)
        Log.i(TAG, "Peer joined group: $peerName (${event.peerId}) in $chatId")
    }

    private suspend fun handleGroupPeerExit(event: GroupDomainEvent.GroupPeerExit) {
        val chatIdBytes = tox.groupGetChatId(event.groupNo)
        val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: return

        checkAndMigrateTemporaryGroup(event.groupNo, chatId)
        checkAndUpdateGroupMetadata(event.groupNo, chatId)

        val group = groupRepository.get(chatId).firstOrNull() ?: return

        val currentSelfPeerId = tox.groupSelfGetPeerId(event.groupNo)
        val isSelf = event.peerId == group.selfPeerId || (currentSelfPeerId >= 0 && event.peerId == currentSelfPeerId)

        if (isSelf) {
            if (event.exitType != ToxGroupExitType.QUIT && event.exitType != ToxGroupExitType.KICK) {
                groupRepository.setConnected(chatId, false)
                groupManager.setConnectionStatus(chatId, GroupConnectionStatus.Reconnecting)
                Log.i(TAG, "Self technical disconnect (${event.exitType}) from group $chatId, scheduling auto reconnect")
                groupManager.scheduleAutoReconnect(chatId, group.groupNumber)
            } else {
                groupRepository.setConnected(chatId, false)
                groupManager.setConnectionStatus(chatId, GroupConnectionStatus.Disconnected)
                Log.i(TAG, "Self left or kicked (${event.exitType}) from group $chatId")
            }
            return
        }

        if (event.exitType == ToxGroupExitType.DISCONNECTED || event.exitType == ToxGroupExitType.TIMEOUT) {
            groupManager.setConnectionStatus(chatId, GroupConnectionStatus.Connecting)
        }

        if (event.exitType == ToxGroupExitType.QUIT || event.exitType == ToxGroupExitType.KICK) {
            val peerName = groupRepository.getPeerNameDirect(chatId, event.peerId) ?: "Unknown"

            groupRepository.deletePeerById(chatId, event.peerId)

            val msgText = if (event.exitType == ToxGroupExitType.QUIT) {
                "[SYSTEM_EVENT:PEER_LEFT|$peerName]"
            } else {
                "[SYSTEM_EVENT:PEER_KICKED|$peerName]"
            }
            val msg = GroupMessage(
                groupChatId = chatId,
                peerId = event.peerId,
                senderName = peerName,
                message = msgText,
                sender = Sender.Received,
                type = MessageType.GroupEvent,
                correlationId = 0,
                timestamp = Date().time,
            )
            groupRepository.addMessage(msg)

            val count = groupRepository.peerCountDirect(chatId)
            groupRepository.setPeerCount(chatId, count)
        }

        Log.i(TAG, "Peer left group: peerId=${event.peerId}, exitType=${event.exitType} in $chatId")
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
            ToxUserStatus.NONE.ordinal -> ltd.evilcorp.domain.model.UserStatus.None
            ToxUserStatus.AWAY.ordinal -> ltd.evilcorp.domain.model.UserStatus.Away
            ToxUserStatus.BUSY.ordinal -> ltd.evilcorp.domain.model.UserStatus.Busy
            else -> ltd.evilcorp.domain.model.UserStatus.None
        }
        groupRepository.setPeerStatus(chatId, event.peerId, userStatus)
    }

    private suspend fun handleGroupPrivacyState(event: GroupDomainEvent.GroupPrivacyStateChanged) {
        val chatIdBytes = tox.groupGetChatId(event.groupNo)
        val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: return
        checkAndMigrateTemporaryGroup(event.groupNo, chatId)
        val state = when (event.privacyState) {
            ToxGroupPrivacyState.PUBLIC -> ltd.evilcorp.domain.model.GroupPrivacyState.Public
            ToxGroupPrivacyState.PRIVATE -> ltd.evilcorp.domain.model.GroupPrivacyState.Private
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
