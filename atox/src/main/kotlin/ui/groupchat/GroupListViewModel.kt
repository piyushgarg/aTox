package ltd.evilcorp.atox.ui.groupchat

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ltd.evilcorp.core.model.Group
import ltd.evilcorp.core.model.GroupPrivacyState
import ltd.evilcorp.domain.feature.GroupManager

class GroupListViewModel @Inject constructor(
    private val scope: CoroutineScope,
    private val groupManager: GroupManager,
) : ViewModel() {
    val groups: LiveData<List<Group>> = groupManager.getAll().asLiveData()

    suspend fun createGroup(name: String, nickname: String, privacyState: GroupPrivacyState, password: String? = null): Int =
        withContext(Dispatchers.IO) {
            val toxPrivacyState = when (privacyState) {
                GroupPrivacyState.Public -> ltd.evilcorp.core.tox.enums.ToxGroupPrivacyState.PUBLIC
                GroupPrivacyState.Private -> ltd.evilcorp.core.tox.enums.ToxGroupPrivacyState.PRIVATE
            }
            groupManager.createGroup(privacyState, name, nickname, password)
        }

    suspend fun leaveGroup(group: Group) = withContext(Dispatchers.IO) {
        groupManager.leaveGroup(group.chatId)
    }

    suspend fun joinByChatId(chatIdHex: String, selfName: String, password: String?): Int =
        withContext(Dispatchers.IO) {
            groupManager.joinByChatId(chatIdHex, selfName, password)
        }

    fun getChatId(groupChatId: String): String? = groupManager.getChatId(groupChatId)

    fun inviteFriend(chatId: String, friendPublicKey: String): Boolean =
        groupManager.inviteFriend(chatId, friendPublicKey)
}
