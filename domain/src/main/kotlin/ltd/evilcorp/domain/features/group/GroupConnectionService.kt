// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.Log
import ltd.evilcorp.domain.core.network.bytesToHex
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.hexToBytes
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState
import ltd.evilcorp.domain.features.group.repository.IGroupRepository

private const val CHAT_ID_RETRY_ATTEMPTS = 100
private const val CHAT_ID_RETRY_DELAY_MS = 10L
private const val ERROR_INVALID_FRIEND = -5
private const val ERROR_INVALID_INVITE = -4
private const val ERROR_INVALID_KEY_LENGTH = -3
private const val ERROR_EXISTS = -2
private const val HEX_KEY_LENGTH = 64
private const val JOIN_TIMEOUT_MS = 45000L

@Singleton
class GroupConnectionService @Inject constructor(
    private val scope: CoroutineScope,
    private val sessionCoordinator: GroupSessionCoordinator,
    private val groupRepository: IGroupRepository,
    private val toxServices: GroupToxServices,
) {
    private val connectionScheduler get() = sessionCoordinator.connectionScheduler
    private val sessionRegistry get() = sessionCoordinator.sessionRegistry
    private val tox get() = toxServices.tox
    private val toxProfile get() = toxServices.profile

    fun reconnectAll() {
        connectionScheduler.reconnectAll()
    }

    fun scheduleAutoReconnect(chatId: String, groupNumber: Int) {
        connectionScheduler.scheduleAutoReconnect(chatId, groupNumber)
    }

    fun cancelReconnect(chatId: String) {
        connectionScheduler.cancelReconnect(chatId)
    }

    fun stopReconnect(chatId: String) {
        connectionScheduler.stopReconnect(chatId)
    }

    fun isBootstrapFriend(pk: String): Boolean =
        connectionScheduler.isBootstrapFriend(pk)

    suspend fun leaveGroup(chatId: String) = withContext(Dispatchers.IO) {
        stopReconnect(chatId)
        val g = groupRepository.get(chatId).firstOrNull()
        g?.let {
            if (it.groupNumber >= 0) {
                tox.groupLeave(it.groupNumber)
            }
            groupRepository.deleteAllPeers(it.chatId)
            groupRepository.deleteByChatId(it.chatId)
            sessionRegistry.removeConnectionStatus(it.chatId)
        }
    }

    suspend fun createGroup(
        privacyState: GroupPrivacyState,
        groupName: String,
        selfName: String,
        password: String? = null,
    ): Int = withContext(Dispatchers.IO) {
        val toxPrivacyState = when (privacyState) {
            GroupPrivacyState.Public -> ToxGroupPrivacyState.PUBLIC
            GroupPrivacyState.Private -> ToxGroupPrivacyState.PRIVATE
        }
        val groupNumber = tox.groupNew(
            toxPrivacyState,
            groupName.toByteArray(),
            selfName.toByteArray(),
        )

        if (groupNumber >= 0) {
            var chatIdBytes = tox.groupGetChatId(groupNumber)
            var attempts = 0
            while (chatIdBytes == null && attempts < CHAT_ID_RETRY_ATTEMPTS) {
                delay(CHAT_ID_RETRY_DELAY_MS)
                chatIdBytes = tox.groupGetChatId(groupNumber)
                attempts++
            }
            val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: "unknown_$groupNumber"

            val selfPeerId = tox.groupSelfGetPeerId(groupNumber)
            val selfRole = tox.groupSelfGetRole(groupNumber)

            if (privacyState == GroupPrivacyState.Private && !password.isNullOrEmpty()) {
                tox.groupSetPassword(groupNumber, password.toByteArray())
            }

            val group = Group(
                chatId = chatId,
                name = groupName,
                privacyState = privacyState,
                passwordProtected = !password.isNullOrEmpty(),
                peerCount = 1,
                selfPeerId = selfPeerId,
                selfRole = selfRole.name,
                groupNumber = groupNumber,
                connected = true,
            )
            groupRepository.add(group)
            sessionRegistry.setConnectionStatus(chatId, GroupConnectionStatus.Connected)

            val ourPeer = GroupPeer(
                groupChatId = chatId,
                peerId = selfPeerId,
                name = selfName,
                publicKey = toxProfile.publicKey.string(),
                role = selfRole.name,
                isOurselves = true,
            )
            groupRepository.addPeer(ourPeer)
        }

        groupNumber
    }

    suspend fun joinGroup(
        friendNo: Int,
        inviteData: ByteArray,
        selfName: String,
        password: String? = null,
    ): Int = withContext(Dispatchers.IO) {
        val groupNumber = tox.groupJoin(
            friendNo,
            inviteData,
            selfName.toByteArray(),
            password?.toByteArray(),
        )

        if (groupNumber >= 0) {
            var chatIdBytes = tox.groupGetChatId(groupNumber)
            var attempts = 0
            while (chatIdBytes == null && attempts < CHAT_ID_RETRY_ATTEMPTS) {
                delay(CHAT_ID_RETRY_DELAY_MS)
                chatIdBytes = tox.groupGetChatId(groupNumber)
                attempts++
            }
            val chatId = chatIdBytes?.bytesToHex()?.lowercase() ?: "unknown_$groupNumber"
            Log.i("GroupConnectionService", "Joined group number $groupNumber, chatId = $chatId (attempts = $attempts)")

            val groupNameBytes = tox.groupGetName(groupNumber)
            val groupName = groupNameBytes?.decodeToString() ?: "Unknown Group"

            val selfPeerId = tox.groupSelfGetPeerId(groupNumber)
            val selfRole = tox.groupSelfGetRole(groupNumber)

            val group = Group(
                chatId = chatId,
                name = groupName,
                selfPeerId = selfPeerId,
                selfRole = selfRole.name,
                groupNumber = groupNumber,
                connected = false,
            )
            groupRepository.add(group)
            sessionRegistry.setConnectionStatus(chatId, GroupConnectionStatus.Connecting)

            val ourPeer = GroupPeer(
                groupChatId = chatId,
                peerId = selfPeerId,
                name = selfName,
                publicKey = toxProfile.publicKey.string(),
                role = selfRole.name,
                isOurselves = true,
            )
            groupRepository.addPeer(ourPeer)
        }

        groupNumber
    }

    suspend fun joinGroupWithBytes(
        friendPublicKey: String,
        inviteDataHex: String,
        selfName: String,
        password: String? = null,
    ): Int {
        val pk = PublicKey(friendPublicKey)
        val friendNo = toxProfile.getFriendNumber(pk)
        if (friendNo < 0) return ERROR_INVALID_FRIEND

        val inviteData = try {
            inviteDataHex.hexToBytes()
        } catch (e: Exception) {
            return ERROR_INVALID_INVITE
        }

        return joinGroup(friendNo, inviteData, selfName, password)
    }

    suspend fun joinByChatId(chatIdHex: String, selfName: String, password: String? = null): Int = withContext(Dispatchers.IO) {
        if (chatIdHex.length != HEX_KEY_LENGTH) return@withContext ERROR_INVALID_KEY_LENGTH
        if (groupRepository.exists(chatIdHex)) return@withContext ERROR_EXISTS

        val chatIdBytes: ByteArray
        try {
            chatIdBytes = chatIdHex.hexToBytes()
        } catch (e: Exception) {
            return@withContext ERROR_INVALID_INVITE
        }

        val groupNumber = tox.groupJoinDirect(
            chatIdBytes,
            selfName.toByteArray(),
            password?.toByteArray(),
        )

        if (groupNumber >= 0) {
            val chatId = chatIdBytes.bytesToHex().lowercase()

            val groupNameBytes = tox.groupGetName(groupNumber)
            val groupName = groupNameBytes?.decodeToString() ?: "Unknown Group"

            val selfPeerId = tox.groupSelfGetPeerId(groupNumber)
            val selfRole = tox.groupSelfGetRole(groupNumber)

            val group = Group(
                chatId = chatId,
                name = groupName,
                selfPeerId = selfPeerId,
                selfRole = selfRole.name,
                groupNumber = groupNumber,
                connected = false,
            )
            groupRepository.add(group)
            sessionRegistry.setConnectionStatus(chatId, GroupConnectionStatus.Connecting)

            scope.launch {
                delay(JOIN_TIMEOUT_MS)
                val g = groupRepository.get(chatId).firstOrNull()
                if (g != null && !g.connected && sessionRegistry.connectionStatuses.value[chatId] == GroupConnectionStatus.Connecting) {
                    Log.w("GroupConnectionService", "Direct join connection timeout for $chatId")
                    sessionRegistry.setConnectionStatus(chatId, GroupConnectionStatus.Disconnected)
                }
            }

            val ourPeer = GroupPeer(
                groupChatId = chatId,
                peerId = selfPeerId,
                name = selfName,
                publicKey = toxProfile.publicKey.string(),
                role = selfRole.name,
                isOurselves = true,
            )
            groupRepository.addPeer(ourPeer)
        }

        groupNumber
    }
}
