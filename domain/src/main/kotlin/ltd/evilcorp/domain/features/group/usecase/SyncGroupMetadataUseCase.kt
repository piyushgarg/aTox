// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.core.network.Log
import ltd.evilcorp.domain.core.network.IToxGroupManager
import ltd.evilcorp.domain.core.network.bytesToHex
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull

/**
 * Use case to sync current group chat name, title, and topic metadata in the background.
 */
class SyncGroupMetadataUseCase @Inject constructor(
    private val groupRepository: IGroupRepository,
    private val tox: IToxGroupManager,
) {
    suspend fun execute(chatId: String) {
        val group = groupRepository.get(chatId).firstOrNull() ?: return
        if (group.groupNumber < 0) return

        if (group.name.isEmpty() || group.name == "Unknown Group" || group.name.startsWith("unknown_")) {
            val groupNameBytes = tox.groupGetName(group.groupNumber)
            val groupName = groupNameBytes?.decodeToString()
            if (!groupName.isNullOrBlank() && groupName != "Unknown Group") {
                groupRepository.setName(chatId, groupName)
            }
        }

        val peers = groupRepository.getPeers(chatId).firstOrNull() ?: emptyList()
        peers.forEach { peer ->
            if (peer.publicKey.isEmpty() && peer.peerId >= 0 && !peer.isOurselves) {
                val peerKeyBytes = tox.groupPeerGetPublicKey(group.groupNumber, peer.peerId)
                val peerKey = peerKeyBytes?.bytesToHex()?.uppercase() ?: ""
                if (peerKey.isNotEmpty()) {
                    val updatedPeer = peer.copy(publicKey = peerKey)
                    groupRepository.addPeer(updatedPeer)
                    Log.i("SyncGroupMetadataUseCase", "Synchronously updated empty publicKey for peer ${peer.name} (${peer.peerId}) -> $peerKey")
                }
            }
        }
    }
}
