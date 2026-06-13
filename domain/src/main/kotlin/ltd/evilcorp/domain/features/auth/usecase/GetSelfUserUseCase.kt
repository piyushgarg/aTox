package ltd.evilcorp.domain.features.auth.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.features.auth.UserManager
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.auth.repository.IProfileRepository

private const val ZERO_PUBLIC_KEY = "0000000000000000000000000000000000000000000000000000000000000000"
private const val ZERO_TOX_ID = "0000000000000000000000000000000000000000000000000000000000000000000000000000"
private const val POLL_INTERVAL_MS = 100L

/**
 * Use case to load the active local Tox user's details and network IDs.
 */
@Suppress("PrintStackTrace")
class GetSelfUserUseCase @Inject constructor(
    private val userManager: UserManager,
    private val tox: ITox,
    private val profileRepository: IProfileRepository,
) {
    val publicKey: PublicKey
        get() = try {
            tox.publicKey
        } catch (e: Exception) {
            System.err.println("GetSelfUserUseCase: Failed to get publicKey: ${e.message}")
            e.printStackTrace()
            PublicKey(ZERO_PUBLIC_KEY)
        }

    val toxId: ToxID
        get() = try {
            tox.toxId
        } catch (e: Exception) {
            System.err.println("GetSelfUserUseCase: Failed to get toxId: ${e.message}")
            e.printStackTrace()
            ToxID(ZERO_TOX_ID)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun execute(): Flow<User?> {
        return flow {
            var lastPk: PublicKey? = null
            var lastProfileId: String? = null
            while (true) {
                val currentPk = try {
                    val pk = tox.publicKey
                    if (pk.string().isNotBlank() && pk.string() != ZERO_PUBLIC_KEY) pk else null
                } catch (e: Exception) {
                    null
                }
                val currentProfileId = try {
                    profileRepository.getActiveProfileId()
                } catch (e: Exception) {
                    null
                }
                if (currentPk != lastPk || currentProfileId != lastProfileId) {
                    lastPk = currentPk
                    lastProfileId = currentProfileId
                    emit(currentPk)
                }
                delay(POLL_INTERVAL_MS)
            }
        }
        .flatMapLatest { pk ->
            if (pk == null) {
                flow { emit(null) }
            } else {
                userManager.get(pk)
            }
        }
    }
}
