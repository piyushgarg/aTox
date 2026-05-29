package ltd.evilcorp.domain.features.auth.usecase

import javax.inject.Inject
import ltd.evilcorp.domain.features.auth.repository.IAvatarRepository

/**
 * Use case to retrieve the local user's avatar file from storage.
 * Uses fully qualified java.io.File to satisfy strict KMP import constraints.
 */
class GetSelfAvatarUseCase @Inject constructor(
    private val avatarRepository: IAvatarRepository,
) {
    fun execute(): java.io.File {
        return avatarRepository.getSelfAvatarFile()
    }
}
