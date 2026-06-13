package ltd.evilcorp.domain.features.group

import kotlinx.coroutines.flow.StateFlow

interface IGroupSessionRegistry {
    var activeGroup: String
    val pendingInvite: StateFlow<GroupInvite?>
    val connectionStatuses: StateFlow<Map<String, GroupConnectionStatus>>
    
    fun setPendingInvite(invite: GroupInvite?)
    fun setConnectionStatus(chatId: String, status: GroupConnectionStatus)
    fun removeConnectionStatus(chatId: String)
    fun clear()
}
