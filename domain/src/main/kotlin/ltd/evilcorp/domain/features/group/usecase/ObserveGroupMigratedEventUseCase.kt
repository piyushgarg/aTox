package ltd.evilcorp.domain.features.group.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow
import ltd.evilcorp.domain.features.group.GroupManager

class ObserveGroupMigratedEventUseCase @Inject constructor(
    private val groupManager: GroupManager
) {
    fun execute(): SharedFlow<Pair<String, String>> = groupManager.groupMigratedEvent
}
