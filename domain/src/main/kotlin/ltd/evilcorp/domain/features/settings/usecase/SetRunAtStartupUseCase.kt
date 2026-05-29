package ltd.evilcorp.domain.features.settings.usecase

import ltd.evilcorp.domain.features.settings.IRunAtStartupController
import ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository
import javax.inject.Inject

/**
 * Use case to toggle run-at-startup preferences and configure receiver components.
 */
class SetRunAtStartupUseCase @Inject constructor(
    private val userSettingsRepository: IUserSettingsRepository,
    private val runAtStartupController: IRunAtStartupController,
) {
    suspend fun execute(enabled: Boolean) {
        userSettingsRepository.updateRunAtStartup(enabled)
        runAtStartupController.setRunAtStartup(enabled)
    }
}
