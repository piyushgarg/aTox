package ltd.evilcorp.domain.features.auth.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.features.auth.UserManager
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.core.model.PublicKey

/**
 * Use case to load the active local Tox user's details and network IDs.
 */
class GetSelfUserUseCase @Inject constructor(
    private val userManager: UserManager,
    private val tox: ITox,
) {
    val publicKey: PublicKey get() = tox.publicKey
    val toxId: ToxID get() = tox.toxId

    fun execute(): Flow<User?> {
        return userManager.get(publicKey)
    }
}
