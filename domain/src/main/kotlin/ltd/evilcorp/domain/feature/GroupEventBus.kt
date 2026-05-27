package ltd.evilcorp.domain.feature

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<GroupDomainEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<GroupDomainEvent> = _events.asSharedFlow()

    fun emit(event: GroupDomainEvent) {
        _events.tryEmit(event)
    }
}
