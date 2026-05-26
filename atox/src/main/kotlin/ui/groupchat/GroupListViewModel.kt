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
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.core.model.GroupPrivacyState
import ltd.evilcorp.domain.feature.GroupManager
import ltd.evilcorp.domain.feature.GroupConnectionStatus

import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val scope: CoroutineScope,
    private val groupManager: GroupManager,
) : ViewModel() {
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
        if (cleanId.length != 64) {
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

    fun getPendingInvite(): ltd.evilcorp.domain.feature.GroupInvite? =
        groupManager.getPendingInvite()

    suspend fun joinWithPendingInvite(friendPublicKey: String, pending: ltd.evilcorp.domain.feature.GroupInvite): Int =
        withContext(Dispatchers.IO) {
            val selfName = groupManager.getDefaultSelfName()
            groupManager.joinGroup(pending.friendNo, pending.inviteData, selfName)
        }
}
