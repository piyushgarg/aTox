package ltd.evilcorp.atox.infrastructure.tox

import android.util.Log
import kotlinx.coroutines.flow.firstOrNull
import ltd.evilcorp.domain.model.GroupPeer
import ltd.evilcorp.domain.tox.bytesToHex

private const val TAG = "GroupDatabaseUpdater"

internal suspend fun GroupDatabaseUpdater.checkAndMigrateTemporaryGroup(groupNo: Int, realChatId: String) {
    val existingChatId = groupRepository.findChatIdByGroupNumber(groupNo)
    if (existingChatId != null && !existingChatId.equals(realChatId, ignoreCase = true)) {
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
