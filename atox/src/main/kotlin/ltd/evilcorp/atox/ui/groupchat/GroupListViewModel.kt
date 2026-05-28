package ltd.evilcorp.atox.ui.groupchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState
import ltd.evilcorp.domain.features.group.GroupManager
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.leaveGroup
import ltd.evilcorp.domain.features.group.joinByChatId
import ltd.evilcorp.domain.features.group.getChatId
import ltd.evilcorp.domain.features.group.getChatIdByGroupNumber
import ltd.evilcorp.domain.features.group.joinGroupWithBytes
import ltd.evilcorp.domain.features.group.inviteFriend
import ltd.evilcorp.domain.features.auth.UserManager
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.features.auth.model.User

import dagger.hilt.android.lifecycle.HiltViewModel

private const val HEX_KEY_LENGTH = 64

@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val groupManager: GroupManager,
    private val userManager: UserManager,
    private val tox: ITox,
) : ViewModel() {

    val publicKey by lazy { tox.publicKey }
    val user: StateFlow<User?> = userManager.get(publicKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val groups: StateFlow<List<Group>> = groupManager.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connectionStatuses: StateFlow<Map<String, GroupConnectionStatus>> = groupManager.connectionStatuses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    suspend fun createGroup(name: String, privacyState: GroupPrivacyState, password: String? = null): Int {
        _isCreating.value = true
        return try {
            withContext(Dispatchers.IO) {
                val nickname = groupManager.getDefaultSelfName()
                groupManager.createGroup(privacyState, name, nickname, password)
            }
        } catch (e: Exception) {
            android.util.Log.e("GroupListViewModel", "Failed to create group: $e")
            -1
        } finally {
            _isCreating.value = false
        }
    }

    suspend fun leaveGroup(group: Group) = withContext(Dispatchers.IO) {
        groupManager.leaveGroup(group.chatId)
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
                val selfName = groupManager.getDefaultSelfName()
                groupManager.joinByChatId(cleanId, selfName, password)
            }
        } catch (e: Exception) {
            android.util.Log.e("GroupListViewModel", "Failed to join group by Chat ID: $e")
            -1
        } finally {
            _isJoining.value = false
        }
    }

    suspend fun getChatId(groupChatId: String): String? = groupManager.getChatId(groupChatId)

    suspend fun getChatIdByGroupNumber(groupNumber: Int): String? = groupManager.getChatIdByGroupNumber(groupNumber)

    suspend fun joinGroupWithBytes(friendPublicKey: String, inviteDataHex: String, password: String?): Int =
        withContext(Dispatchers.IO) {
            val selfName = groupManager.getDefaultSelfName()
            groupManager.joinGroupWithBytes(friendPublicKey, inviteDataHex, selfName, password)
        }

    suspend fun inviteFriend(chatId: String, friendPublicKey: String): Boolean =
        groupManager.inviteFriend(chatId, friendPublicKey)

    fun getPendingInvite(): ltd.evilcorp.domain.features.group.GroupInvite? =
        groupManager.getPendingInvite()

    suspend fun joinWithPendingInvite(pending: ltd.evilcorp.domain.features.group.GroupInvite): Int =
        withContext(Dispatchers.IO) {
            val selfName = groupManager.getDefaultSelfName()
            groupManager.joinGroup(pending.friendNo, pending.inviteData, selfName)
        }

    suspend fun joinGroupFromChat(
        friendPublicKey: String,
        chatIdOrBytes: String,
        groupName: String,
    ): String? {
        val groupNumber = if (chatIdOrBytes.length == HEX_KEY_LENGTH) {
            val pending = getPendingInvite()
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
