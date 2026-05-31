// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.group

import ltd.evilcorp.domain.features.chat.ChatManager

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.group.model.Group

private const val GROUP_MIGRATION_FLOW_CAPACITY = 10

@Singleton
class GroupManager @Inject constructor(
    internal val scope: CoroutineScope,
    private val repositories: GroupDataRepositories,
    internal val chatManager: ChatManager,
    private val toxServices: GroupToxServices,
    private val sessionCoordinator: GroupSessionCoordinator,
    private val services: GroupServices,
) {
    internal val groupRepository get() = repositories.group
    internal val contactRepository get() = repositories.contact
    internal val messageRepository get() = repositories.message
    private val sessionRegistry get() = sessionCoordinator.sessionRegistry
    private val tox get() = toxServices.tox
    private val toxProfile get() = toxServices.profile
    private val groupConnectionService get() = services.connection
    private val groupMessagingService get() = services.messaging

    var activeGroup: String
        get() = sessionRegistry.activeGroup
        set(value) {
            sessionRegistry.activeGroup = value
            if (value.isNotEmpty()) {
                scope.launch {
                    groupRepository.setHasUnreadMessages(value, false)
                }
            }
        }

    val pendingInvite: Flow<GroupInvite?> = sessionRegistry.pendingInvite
    val connectionStatuses: Flow<Map<String, GroupConnectionStatus>> = sessionRegistry.connectionStatuses

    private val _groupMigratedEvent = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = GROUP_MIGRATION_FLOW_CAPACITY)
    val groupMigratedEvent: SharedFlow<Pair<String, String>> = _groupMigratedEvent.asSharedFlow()

    fun connectionStatus(chatId: String): GroupConnectionStatus =
        sessionRegistry.connectionStatuses.value[chatId] ?: GroupConnectionStatus.Disconnected

    fun setConnectionStatus(chatId: String, status: GroupConnectionStatus) {
        sessionRegistry.setConnectionStatus(chatId, status)
    }

    fun removeConnectionStatus(chatId: String) {
        sessionRegistry.removeConnectionStatus(chatId)
    }

    fun setPendingInvite(invite: GroupInvite?) {
        sessionRegistry.setPendingInvite(invite)
    }

    fun getPendingInvite(): GroupInvite? = sessionRegistry.pendingInvite.value

    fun cancelReconnect(chatId: String) {
        groupConnectionService.cancelReconnect(chatId)
    }

    fun stopReconnect(chatId: String) {
        groupConnectionService.stopReconnect(chatId)
    }

    fun isBootstrapFriend(pk: String): Boolean =
        groupConnectionService.isBootstrapFriend(pk)

    fun notifyGroupMigrated(oldChatId: String, newChatId: String) {
        scope.launch {
            _groupMigratedEvent.emit(Pair(oldChatId, newChatId))
        }
    }

    fun scheduleAutoReconnect(chatId: String, groupNumber: Int) {
        groupConnectionService.scheduleAutoReconnect(chatId, groupNumber)
    }

    fun resendPendingMessages(chatId: String) {
        groupMessagingService.resendPendingMessages(chatId)
    }

    fun getDefaultSelfName(): String {
        return toxProfile.getName().ifEmpty { "User" }
    }

    fun getAll(): Flow<List<Group>> = groupRepository.getAll()
    fun get(chatId: String): Flow<Group?> = groupRepository.get(chatId)
}
