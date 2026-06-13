package ltd.evilcorp.atox.infrastructure.tox

import android.util.Log
import java.util.Date
import kotlinx.coroutines.flow.firstOrNull
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.core.network.enums.ToxGroupExitType
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.GroupInvite
import ltd.evilcorp.domain.features.group.GroupDomainEvent
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import ltd.evilcorp.domain.features.transfer.repository.IFileTransferRepository
import ltd.evilcorp.domain.core.network.bytesToHex

private const val TAG = "GroupDatabaseUpdater"

internal suspend fun GroupDatabaseUpdater.checkAndMigrateTemporaryGroup(groupNo: Int, realChatId: String) {
    val existingChatId = groupRepository.findChatIdByGroupNumber(groupNo)
    if (existingChatId != null && existingChatId.startsWith("unknown_") && !existingChatId.equals(realChatId, ignoreCase = true)) {
        Log.i(TAG, "Migrating temporary group from $existingChatId to $realChatId")
        try {
            val tempGroup = groupRepository.getDirect(existingChatId)
            if (tempGroup != null) {
                val newGroup = tempGroup.copy(chatId = realChatId)
                val peers = groupRepository.getPeers(existingChatId).firstOrNull() ?: emptyList()
                val messages = groupRepository.getMessages(existingChatId).firstOrNull() ?: emptyList()

                groupRepository.deleteAllPeers(existingChatId)
                groupRepository.deleteMessages(existingChatId)
                groupRepository.deleteByChatId(existingChatId)
                groupRepository.add(newGroup)

                peers.forEach { peer ->
                    groupRepository.addPeer(peer.copy(groupChatId = realChatId))
                }
                messages.forEach { msg ->
                    groupRepository.addMessage(msg.copy(groupChatId = realChatId))
                }

                val oldStatus = groupManager.connectionStatus(existingChatId)
                groupManager.setConnectionStatus(realChatId, oldStatus)
                groupManager.notifyGroupMigrated(existingChatId, realChatId)
                Log.i(TAG, "Successfully migrated groupNo=$groupNo to $realChatId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate temporary group from $existingChatId to $realChatId", e)
        }
    }
}

internal suspend fun GroupDatabaseUpdater.checkAndUpdateGroupMetadata(groupNo: Int, chatId: String) {
    val group = groupRepository.get(chatId).firstOrNull() ?: return

    if (group.name.isEmpty() || group.name == "Unknown Group" || group.name.startsWith("unknown_")) {
        val groupNameBytes = tox.groupGetName(groupNo)
        val groupName = groupNameBytes?.decodeToString()
        if (!groupName.isNullOrBlank() && groupName != "Unknown Group") {
            groupRepository.setName(chatId, groupName)
        }
    }

    val currentSelfPeerId = tox.groupSelfGetPeerId(groupNo)
    val currentSelfRole = tox.groupSelfGetRole(groupNo)
    if (currentSelfPeerId >= 0 && (group.selfPeerId != currentSelfPeerId || group.selfRole != currentSelfRole.name)) {
        groupRepository.setSelfPeerId(chatId, currentSelfPeerId)
        groupRepository.setSelfRole(chatId, currentSelfRole.name)

        groupRepository.deletePeerById(chatId, -1)
        val ourPk = tox.publicKey.string()
        groupRepository.deletePeerByPublicKey(chatId, ourPk)
        groupRepository.deletePeerById(chatId, currentSelfPeerId)

        val ourPeer = GroupPeer(
            groupChatId = chatId,
            peerId = currentSelfPeerId,
            name = tox.getName(),
            publicKey = ourPk,
            role = currentSelfRole.name,
            isOurselves = true,
        )
        groupRepository.addPeer(ourPeer)
    }

    val count = groupRepository.peerCountDirect(chatId)
    groupRepository.setPeerCount(chatId, count)

    val peers = groupRepository.getPeers(chatId).firstOrNull() ?: emptyList()
    peers.forEach { peer ->
        if (peer.publicKey.isEmpty() && peer.peerId >= 0 && !peer.isOurselves) {
            val peerKeyBytes = tox.groupPeerGetPublicKey(groupNo, peer.peerId)
            val peerKey = peerKeyBytes?.bytesToHex()?.uppercase() ?: ""
            if (peerKey.isNotEmpty()) {
                val updatedPeer = peer.copy(publicKey = peerKey)
                groupRepository.addPeer(updatedPeer)
                Log.i(TAG, "Updated empty publicKey for peer ${peer.name} (${peer.peerId}) -> $peerKey")
            }
        }
    }
}

internal suspend fun GroupDatabaseUpdater.handleGroupInvite(
    event: GroupDomainEvent.GroupInvite,
    messageRepository: IMessageRepository
) {
    val friendPkObject = tox.getFriendPublicKey(event.friendNo)
    val friendPk = friendPkObject?.string()?.lowercase() ?: ""

    // Layer 3 (revised): Background speculative join
    // We join the group in the background to get its chatId without irreparably breaking the invite.
    // If it's a known group, preJoinInvite processes it completely and we can just return!
    val preJoin = groupConnectionService.preJoinInvite(
        event.friendNo,
        friendPk,
        event.inviteData,
        groupManager.getDefaultSelfName()
    )
    if (preJoin.success && preJoin.isKnownGroup) {
        Log.i(TAG, "Layer 3: Auto-accepted invite for KNOWN group ${preJoin.chatId}. Reconnected automatically!")
        return
    }

    // Normal handling: show invite to user (for unknown groups, or if pre-join failed)
    try {
        if (friendPk.isNotEmpty()) {
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

internal suspend fun GroupDatabaseUpdater.handleGroupMessage(
    event: GroupDomainEvent.GroupMessage,
    fileTransferRepository: IFileTransferRepository
) {
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
        parseAndRegisterGroupFile(chatId, event.message, fileTransferRepository)?.let {
            corrId = it
        }
    } else if (isVoice) {
        parseAndRegisterGroupVoice(chatId, event.message, fileTransferRepository)?.let {
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

internal suspend fun GroupDatabaseUpdater.parseAndRegisterGroupFile(
    chatId: String,
    message: String,
    fileTransferRepository: IFileTransferRepository
): Int? {
    try {
        val parts = message.removePrefix("[FILE:").removeSuffix("]").split("|")
        if (parts.size >= 3) {
            val fileName = parts[0]
            val fileSize = parts[1].toLong()
            val originalCorrId = parts[2].toInt()

            val ft = FileTransfer(
                publicKey = chatId,
                fileNumber = originalCorrId,
                fileKind = ltd.evilcorp.domain.features.transfer.model.FileKind.Data.ordinal,
                fileSize = fileSize,
                fileName = fileName,
                outgoing = false,
                progress = ltd.evilcorp.domain.features.transfer.model.FT_NOT_STARTED,
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

internal suspend fun GroupDatabaseUpdater.parseAndRegisterGroupVoice(
    chatId: String,
    message: String,
    fileTransferRepository: IFileTransferRepository
): Int? {
    try {
        val parts = message.removePrefix("[VOICE:").removeSuffix("]").split("|")
        if (parts.size >= 2) {
            val duration = parts[0].toInt()
            val originalCorrId = parts[1].toInt()

            val ft = FileTransfer(
                publicKey = chatId,
                fileNumber = originalCorrId,
                fileKind = ltd.evilcorp.domain.features.transfer.model.FileKind.Data.ordinal,
                fileSize = duration * 1000L,
                fileName = "voice_message_${originalCorrId}.opus",
                outgoing = false,
                progress = ltd.evilcorp.domain.features.transfer.model.FT_NOT_STARTED,
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

internal suspend fun GroupDatabaseUpdater.handleGroupPeerJoin(event: GroupDomainEvent.GroupPeerJoin) {
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
            role = if (event.peerId == 0) "FOUNDER" else "USER"
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

    // Flush any pending messages if a peer joins. 
    // This is crucial for healing split-brain scenarios where we were online alone,
    // queued a message, and never got a GroupConnected event when the other peer joined.
    Log.i(TAG, "Calling groupManager.resendPendingMessages for $chatId")
    groupManager.resendPendingMessages(chatId)

    Log.i(TAG, "Peer joined group: $peerName (${event.peerId}) in $chatId")
}

internal suspend fun GroupDatabaseUpdater.handleGroupPeerExit(event: GroupDomainEvent.GroupPeerExit) {
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

