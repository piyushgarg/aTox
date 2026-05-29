package ltd.evilcorp.domain.features.group.usecase

import ltd.evilcorp.domain.features.group.GroupConnectionService
import javax.inject.Inject

/**
 * Use case to leave a specific group chat.
 */
class LeaveGroupUseCase @Inject constructor(
    private val groupConnectionService: GroupConnectionService,
) {
    suspend fun execute(chatId: String) {
        groupConnectionService.leaveGroup(chatId)
    }
}
