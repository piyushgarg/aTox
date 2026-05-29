package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.features.group.GroupConnectionService
import ltd.evilcorp.domain.features.group.GroupInvite
import ltd.evilcorp.domain.features.group.GroupManager
import javax.inject.Inject

sealed interface JoinAction {
    data class ByChatId(val chatIdHex: String, val password: String?) : JoinAction
    data class ByBytes(val friendPublicKey: String, val inviteDataHex: String, val password: String?) : JoinAction
    data class ByPendingInvite(val pending: GroupInvite) : JoinAction
}

/**
 * Use case to join a group chat session via various methods (Chat ID, invitation bytes, pending invite).
 */
class JoinGroupUseCase @Inject constructor(
    private val groupConnectionService: GroupConnectionService,
    private val groupManager: GroupManager,
) {
    suspend fun execute(action: JoinAction): Int {
        val selfName = groupManager.getDefaultSelfName()
        return when (action) {
            is JoinAction.ByChatId -> groupConnectionService.joinByChatId(action.chatIdHex, selfName, action.password)
            is JoinAction.ByBytes -> groupConnectionService.joinGroupWithBytes(action.friendPublicKey, action.inviteDataHex, selfName, action.password)
            is JoinAction.ByPendingInvite -> groupConnectionService.joinGroup(action.pending.friendNo, action.pending.inviteData, selfName)
        }
    }
}
