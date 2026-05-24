package ltd.evilcorp.atox.ui.groupchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.core.model.GroupPrivacyState
import ltd.evilcorp.domain.feature.GroupManager
import ltd.evilcorp.domain.feature.GroupConnectionStatus

class GroupListViewModel @Inject constructor(
    private val scope: CoroutineScope,
    private val groupManager: GroupManager,
) : ViewModel() {
    val groups: StateFlow<List<Group>> = groupManager.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connectionStatuses: StateFlow<Map<String, GroupConnectionStatus>> = groupManager.connectionStatuses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    suspend fun createGroup(name: String, privacyState: GroupPrivacyState, password: String? = null): Int =
        withContext(Dispatchers.IO) {
            val nickname = groupManager.getDefaultSelfName()
            groupManager.createGroup(privacyState, name, nickname, password)
        }

    suspend fun leaveGroup(group: Group) = withContext(Dispatchers.IO) {
        groupManager.leaveGroup(group.chatId)
    }

    suspend fun joinByChatId(chatIdHex: String, password: String?): Int =
        withContext(Dispatchers.IO) {
            val selfName = groupManager.getDefaultSelfName()
            groupManager.joinByChatId(chatIdHex, selfName, password)
        }

    fun getChatId(groupChatId: String): String? = groupManager.getChatId(groupChatId)

    fun getChatIdByGroupNumber(groupNumber: Int): String? = groupManager.getChatIdByGroupNumber(groupNumber)

    suspend fun joinGroupWithBytes(friendPublicKey: String, inviteDataHex: String, password: String?): Int =
        withContext(Dispatchers.IO) {
            val selfName = groupManager.getDefaultSelfName()
            groupManager.joinGroupWithBytes(friendPublicKey, inviteDataHex, selfName, password)
        }

    fun inviteFriend(chatId: String, friendPublicKey: String): Boolean =
        groupManager.inviteFriend(chatId, friendPublicKey)

    fun getPendingInvite(): ltd.evilcorp.domain.feature.GroupInvite? =
        groupManager.getPendingInvite()

    suspend fun joinWithPendingInvite(friendPublicKey: String, pending: ltd.evilcorp.domain.feature.GroupInvite): Int =
        withContext(Dispatchers.IO) {
            val selfName = groupManager.getDefaultSelfName()
            groupManager.joinGroup(pending.friendNo, pending.inviteData, selfName)
        }
}
