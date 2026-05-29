package ltd.evilcorp.domain.features.group.usecase

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.group.GroupManager
import ltd.evilcorp.domain.features.group.model.Group
import javax.inject.Inject

/**
 * Use case to query active group chat details and trigger background metadata synchronization.
 */
class GetGroupChatUseCase @Inject constructor(
    private val groupManager: GroupManager,
) {
    fun execute(chatId: String): Flow<Group?> {
        return groupManager.get(chatId)
    }
}
