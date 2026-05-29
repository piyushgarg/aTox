package ltd.evilcorp.domain.features.group.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.GroupManager
import javax.inject.Inject

/**
 * Use case to retrieve the active group chat network connection status.
 */
class GetGroupConnectionStatusUseCase @Inject constructor(
    private val groupManager: GroupManager,
) {
    fun execute(chatId: String): Flow<GroupConnectionStatus> {
        return groupManager.connectionStatuses.map { statuses ->
            statuses[chatId] ?: GroupConnectionStatus.Disconnected
        }
    }
}
