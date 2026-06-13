package ltd.evilcorp.core.platform.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.GroupInvite
import ltd.evilcorp.domain.features.group.IGroupSessionRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupSessionRegistryImpl @Inject constructor() : IGroupSessionRegistry {
    override var activeGroup = ""

    private val _pendingInvite = MutableStateFlow<GroupInvite?>(null)
    override val pendingInvite: StateFlow<GroupInvite?> = _pendingInvite.asStateFlow()

    private val _connectionStatuses = MutableStateFlow<Map<String, GroupConnectionStatus>>(emptyMap())
    override val connectionStatuses: StateFlow<Map<String, GroupConnectionStatus>> = _connectionStatuses.asStateFlow()

    override fun setPendingInvite(invite: GroupInvite?) {
        _pendingInvite.value = invite
    }

    override fun setConnectionStatus(chatId: String, status: GroupConnectionStatus) {
        _connectionStatuses.value += (chatId to status)
    }

    override fun removeConnectionStatus(chatId: String) {
        _connectionStatuses.value -= chatId
    }

    override fun clear() {
        activeGroup = ""
        _pendingInvite.value = null
        _connectionStatuses.value = emptyMap()
    }
}
