package ltd.evilcorp.atox.infrastructure.sharing

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ltd.evilcorp.atox.SharedContent

@Singleton
class SharedContentManager @Inject constructor() {
    private val _sharedContent = MutableStateFlow<SharedContent?>(null)
    val sharedContent: StateFlow<SharedContent?> = _sharedContent.asStateFlow()

    fun setSharedContent(content: SharedContent?) {
        _sharedContent.value = content
    }

    fun clear() {
        _sharedContent.value = null
    }
}
