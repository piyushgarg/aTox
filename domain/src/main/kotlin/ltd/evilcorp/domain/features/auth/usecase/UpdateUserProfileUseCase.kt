package ltd.evilcorp.domain.features.auth.usecase

import javax.inject.Inject
import ltd.evilcorp.domain.features.auth.UserManager
import ltd.evilcorp.domain.features.contacts.model.UserStatus

sealed interface ProfileAction {
    data class Name(val name: String) : ProfileAction
    data class StatusMessage(val message: String) : ProfileAction
    data class Status(val status: UserStatus) : ProfileAction
}

/**
 * Use case to update the self user's name, status message, or presence status.
 */
class UpdateUserProfileUseCase @Inject constructor(
    private val userManager: UserManager,
) {
    suspend fun execute(action: ProfileAction) {
        when (action) {
            is ProfileAction.Name -> userManager.setName(action.name)
            is ProfileAction.StatusMessage -> userManager.setStatusMessage(action.message)
            is ProfileAction.Status -> userManager.setStatus(action.status)
        }
    }
}
