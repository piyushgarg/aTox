package ltd.evilcorp.domain.features.auth.usecase

import javax.inject.Inject
import ltd.evilcorp.domain.core.network.IToxStarter
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus

class InitializeToxUseCase @Inject constructor(
    private val toxStarter: IToxStarter
) {
    fun execute(password: String? = null): ToxSaveStatus {
        return toxStarter.tryLoadTox(password)
    }
}
