package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.features.group.GroupConnectionService
import ltd.evilcorp.domain.features.group.GroupManager
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState
import javax.inject.Inject

/**
 * Use case to create a new group chat session with custom configurations.
 */
class CreateGroupUseCase @Inject constructor(
    private val groupConnectionService: GroupConnectionService,
    private val groupManager: GroupManager,
) {
    suspend fun execute(name: String, privacyState: GroupPrivacyState, password: String? = null): Int {
        val nickname = groupManager.getDefaultSelfName()
        return groupConnectionService.createGroup(privacyState, name, nickname, password)
    }
}
