package ltd.evilcorp.domain.features.auth.usecase

import javax.inject.Inject

/**
 * Use case to retrieve the local user's avatar File URI as a platform-agnostic String.
 */
class GetSelfAvatarUriUseCase @Inject constructor(
    private val getSelfAvatarUseCase: GetSelfAvatarUseCase,
) {
    fun execute(): String? {
        val file = getSelfAvatarUseCase.execute()
        return if (file.exists()) {
            file.toURI().toString()
        } else {
            null
        }
    }
}
