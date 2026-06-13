package ltd.evilcorp.domain.fakes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ltd.evilcorp.domain.features.group.GroupInvite
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.IGroupSessionRegistry

class FakeGroupSessionRegistry : IGroupSessionRegistry {
    override var activeGroup: String = ""
    private val _pendingInvite = MutableStateFlow<GroupInvite?>(null)
    override val pendingInvite: StateFlow<GroupInvite?> = _pendingInvite
    private val _connectionStatuses = MutableStateFlow<Map<String, GroupConnectionStatus>>(emptyMap())
    override val connectionStatuses: StateFlow<Map<String, GroupConnectionStatus>> = _connectionStatuses

    override fun setPendingInvite(invite: GroupInvite?) {
        _pendingInvite.value = invite
    }

    override fun setConnectionStatus(chatId: String, status: GroupConnectionStatus) {
        _connectionStatuses.value = _connectionStatuses.value + (chatId to status)
    }

    override fun removeConnectionStatus(chatId: String) {
        _connectionStatuses.value = _connectionStatuses.value - chatId
    }

    override fun clear() {
        activeGroup = ""
        _pendingInvite.value = null
        _connectionStatuses.value = emptyMap()
    }
}
