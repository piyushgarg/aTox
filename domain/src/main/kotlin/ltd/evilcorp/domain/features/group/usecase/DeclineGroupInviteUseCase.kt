package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.features.group.IGroupSessionRegistry
import javax.inject.Inject

class DeclineGroupInviteUseCase @Inject constructor(
    private val sessionRegistry: IGroupSessionRegistry,
) {
    fun execute() {
        sessionRegistry.setPendingInvite(null)
    }
}
