package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.features.group.GroupConnectionService
import ltd.evilcorp.domain.features.group.IGroupSessionRegistry
import ltd.evilcorp.domain.core.network.bytesToHex
import javax.inject.Inject

class DeclineGroupInviteUseCase @Inject constructor(
    private val groupConnectionService: GroupConnectionService,
    private val sessionRegistry: IGroupSessionRegistry,
) {
    fun execute(inviteDataHex: String? = null) {
        val hexToDecline = inviteDataHex ?: sessionRegistry.pendingInvite.value?.inviteData?.bytesToHex()
        if (hexToDecline != null) {
            groupConnectionService.declineInvite(hexToDecline)
        }
        if (inviteDataHex == null) {
            sessionRegistry.setPendingInvite(null)
        }
    }
}
