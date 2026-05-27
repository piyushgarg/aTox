package ltd.evilcorp.domain.usecase

import javax.inject.Inject
import ltd.evilcorp.domain.tox.IToxStarter
import ltd.evilcorp.domain.tox.save.ToxSaveStatus

class InitializeToxUseCase @Inject constructor(
    private val toxStarter: IToxStarter
) {
    fun execute(password: String? = null): ToxSaveStatus {
        return toxStarter.tryLoadTox(password)
    }
}
