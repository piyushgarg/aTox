package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.core.network.IToxProfile
import ltd.evilcorp.domain.features.group.GroupConnectionService
import ltd.evilcorp.domain.features.group.IGroupSessionRegistry
import javax.inject.Inject

class AcceptGroupInviteUseCase @Inject constructor(
    private val sessionRegistry: IGroupSessionRegistry,
    private val toxProfile: IToxProfile,
    private val groupConnectionService: GroupConnectionService,
) {
    suspend fun execute() {
        val invite = sessionRegistry.pendingInvite.value ?: return
        sessionRegistry.setPendingInvite(null)
        val selfName = toxProfile.getName()
        groupConnectionService.joinGroup(invite.friendNo, invite.inviteData, selfName)
    }
}
