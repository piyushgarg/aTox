package ltd.evilcorp.atox.ui.navigation

import androidx.lifecycle.ViewModel
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import ltd.evilcorp.atox.infrastructure.tox.ToxStarter
import ltd.evilcorp.core.tox.save.ToxSaveStatus
import ltd.evilcorp.domain.tox.ITox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

sealed interface LaunchUiState {
    object Loading : LaunchUiState
    object Timeout : LaunchUiState
    data class Success(val status: ToxSaveStatus) : LaunchUiState
}

sealed interface UnlockUiState {
    object Idle : UnlockUiState
    object Loading : UnlockUiState
    object Error : UnlockUiState
    object Success : UnlockUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val tox: ITox,
    private val toxStarter: ToxStarter,
) : ViewModel() {
    val publicKey by lazy { tox.publicKey }

    val launchState = MutableStateFlow<LaunchUiState>(LaunchUiState.Loading)
    val unlockState = MutableStateFlow<UnlockUiState>(UnlockUiState.Idle)

    fun isToxRunning(): Boolean = tox.started

    fun tryLoadTox(password: String?): ToxSaveStatus {
        return toxStarter.tryLoadTox(password)
    }

    suspend fun loadToxAsync(password: String?) {
        launchState.value = LaunchUiState.Loading
        try {
            withTimeout(10_000L) {
                val result = withContext(Dispatchers.IO) {
                    toxStarter.tryLoadTox(password)
                }
                launchState.value = LaunchUiState.Success(result)
            }
        } catch (e: TimeoutCancellationException) {
            launchState.value = LaunchUiState.Timeout
        } catch (e: Exception) {
            launchState.value = LaunchUiState.Success(ToxSaveStatus.BadFormat)
        }
    }

    suspend fun unlockProfileAsync(password: String): Boolean {
        unlockState.value = UnlockUiState.Loading
        val success = withContext(Dispatchers.IO) {
            toxStarter.tryLoadTox(password) == ToxSaveStatus.Ok
        }
        if (success) {
            unlockState.value = UnlockUiState.Success
            return true
        } else {
            unlockState.value = UnlockUiState.Error
            return false
        }
    }

    fun clearUnlockError() {
        if (unlockState.value is UnlockUiState.Error) {
            unlockState.value = UnlockUiState.Idle
        }
    }

    fun quitTox() {
        toxStarter.stopTox()
    }
}
