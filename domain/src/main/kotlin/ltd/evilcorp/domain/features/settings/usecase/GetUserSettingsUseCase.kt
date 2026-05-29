package ltd.evilcorp.domain.features.settings.usecase

import kotlinx.coroutines.flow.StateFlow
import ltd.evilcorp.domain.features.settings.model.UserSettings
import ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository
import javax.inject.Inject

/**
 * Use case to load the active user's system settings.
 */
class GetUserSettingsUseCase @Inject constructor(
    private val userSettingsRepository: IUserSettingsRepository,
) {
    val settings: StateFlow<UserSettings> get() = userSettingsRepository.settings
}
