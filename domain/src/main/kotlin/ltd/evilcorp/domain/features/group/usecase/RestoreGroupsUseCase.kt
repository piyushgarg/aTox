package ltd.evilcorp.domain.features.group.usecase

import kotlinx.coroutines.flow.first
import ltd.evilcorp.domain.features.group.GroupSessionCoordinator
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import javax.inject.Inject

class RestoreGroupsUseCase @Inject constructor(
    private val groupRepository: IGroupRepository,
    private val sessionCoordinator: GroupSessionCoordinator,
) {
    @Suppress("RedundantSuspendModifier")
    suspend fun execute() {
        val groups = groupRepository.getAll().first()
        groups.forEach { group ->
            if (group.groupNumber != -1) {
                sessionCoordinator.connectionScheduler.scheduleAutoReconnect(group.chatId, group.groupNumber)
            }
        }
    }
}
