// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.groupchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.GroupInvite
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.group.usecase.GetGroupsUseCase
import ltd.evilcorp.domain.features.group.usecase.GetGroupConnectionStatusesUseCase
import ltd.evilcorp.domain.features.group.usecase.CreateGroupUseCase
import ltd.evilcorp.domain.features.group.usecase.LeaveGroupUseCase
import ltd.evilcorp.domain.features.group.usecase.JoinGroupUseCase
import ltd.evilcorp.domain.features.group.usecase.JoinAction
import kotlinx.coroutines.flow.firstOrNull
import ltd.evilcorp.domain.features.group.usecase.GetGroupChatIdUseCase
import ltd.evilcorp.domain.features.group.usecase.ChatIdQuery
import ltd.evilcorp.domain.features.group.usecase.InviteFriendToGroupUseCase
import ltd.evilcorp.domain.features.group.usecase.GetGroupInviteUseCase

import dagger.hilt.android.lifecycle.HiltViewModel

private const val HEX_KEY_LENGTH = 64

@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val getGroupsUseCase: GetGroupsUseCase,
    private val getGroupConnectionStatusesUseCase: GetGroupConnectionStatusesUseCase,
    private val getSelfUserUseCase: GetSelfUserUseCase,
    private val createGroupUseCase: CreateGroupUseCase,
    private val leaveGroupUseCase: LeaveGroupUseCase,
    private val joinGroupUseCase: JoinGroupUseCase,
    private val getGroupChatIdUseCase: GetGroupChatIdUseCase,
    private val inviteFriendToGroupUseCase: InviteFriendToGroupUseCase,
    private val getGroupInviteUseCase: GetGroupInviteUseCase,
) : ViewModel() {

    val publicKey by lazy { getSelfUserUseCase.publicKey }
    val user: StateFlow<User?> = getSelfUserUseCase.execute()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val groups: StateFlow<List<Group>> = getGroupsUseCase.execute()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connectionStatuses: StateFlow<Map<String, GroupConnectionStatus>> = getGroupConnectionStatusesUseCase.execute()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    suspend fun createGroup(name: String, privacyState: GroupPrivacyState, password: String? = null): Int {
        _isCreating.value = true
        return try {
            withContext(Dispatchers.IO) {
                createGroupUseCase.execute(name, privacyState, password)
            }
        } catch (e: Exception) {
            android.util.Log.e("GroupListViewModel", "Failed to create group: $e")
            -1
        } finally {
            _isCreating.value = false
        }
    }

    suspend fun leaveGroup(group: Group) = withContext(Dispatchers.IO) {
        leaveGroupUseCase.execute(group.chatId)
    }

    private val _isJoining = MutableStateFlow(false)
    val isJoining: StateFlow<Boolean> = _isJoining.asStateFlow()

    fun validateChatId(chatIdHex: String): String? {
        val cleanId = chatIdHex.trim().replace("\\s".toRegex(), "")
        if (cleanId.isEmpty()) {
            return "Chat ID is required"
        }
        if (cleanId.length != HEX_KEY_LENGTH) {
            return "Chat ID must be 64 hex characters (32 bytes)"
        }
        val isHex = cleanId.all { it in "0123456789abcdefABCDEF" }
        if (!isHex) {
            return "Chat ID must contain only hexadecimal characters"
        }
        return null
    }

    suspend fun joinByChatId(chatIdHex: String, password: String?): Int {
        _isJoining.value = true
        return try {
            val cleanId = chatIdHex.trim().replace("\\s".toRegex(), "")
            withContext(Dispatchers.IO) {
                joinGroupUseCase.execute(JoinAction.ByChatId(cleanId, password))
            }
        } catch (e: Exception) {
            android.util.Log.e("GroupListViewModel", "Failed to join group by Chat ID: $e")
            -1
        } finally {
            _isJoining.value = false
        }
    }

    suspend fun getChatId(groupChatId: String): String? =
        getGroupChatIdUseCase.execute(ChatIdQuery.ByGroupChatId(groupChatId))

    suspend fun getChatIdByGroupNumber(groupNumber: Int): String? =
        getGroupChatIdUseCase.execute(ChatIdQuery.ByGroupNumber(groupNumber))

    suspend fun joinGroupWithBytes(friendPublicKey: String, inviteDataHex: String, password: String?): Int =
        withContext(Dispatchers.IO) {
            joinGroupUseCase.execute(JoinAction.ByBytes(friendPublicKey, inviteDataHex, password))
        }

    suspend fun inviteFriend(chatId: String, friendPublicKey: String): Boolean {
        return try {
            inviteFriendToGroupUseCase.execute(chatId, friendPublicKey)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getPendingInviteDirect(): GroupInvite? {
        return getGroupInviteUseCase.execute().firstOrNull()
    }

    suspend fun joinWithPendingInvite(pending: GroupInvite): Int =
        withContext(Dispatchers.IO) {
            joinGroupUseCase.execute(JoinAction.ByPendingInvite(pending))
        }

    suspend fun joinGroupFromChat(
        friendPublicKey: String,
        chatIdOrBytes: String,
        groupName: String,
    ): String? {
        val groupNumber = if (chatIdOrBytes.length == HEX_KEY_LENGTH) {
            val pendingFlow = getGroupInviteUseCase.execute()
            val pending = pendingFlow.stateIn(viewModelScope).value
            if (pending != null && pending.groupName.equals(groupName, ignoreCase = true)) {
                joinWithPendingInvite(pending)
            } else {
                joinByChatId(chatIdOrBytes, null)
            }
        } else {
            joinGroupWithBytes(friendPublicKey, chatIdOrBytes, null)
        }

        if (groupNumber >= 0) {
            return if (chatIdOrBytes.length == HEX_KEY_LENGTH) {
                chatIdOrBytes
            } else {
                getChatIdByGroupNumber(groupNumber) ?: ""
            }
        }
        return null
    }
}
