package ltd.evilcorp.atox.ui.navigation

import androidx.lifecycle.ViewModel
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import ltd.evilcorp.atox.infrastructure.tox.ToxStarter
import ltd.evilcorp.domain.tox.save.ToxSaveStatus
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

    fun enableBiometric(context: android.content.Context, password: String): Boolean {
        return try {
            val cipher = ltd.evilcorp.atox.infrastructure.security.BiometricCipherHelper.getInitializedCipherForEncryption()
            val encryptedBytes = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            ltd.evilcorp.atox.infrastructure.security.BiometricStorage.saveEncryptedPassword(context, encryptedBytes, iv)
            true
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Failed to enable biometric: $e")
            false
        }
    }

    fun decryptPassword(context: android.content.Context, cipher: javax.crypto.Cipher): String? {
        return try {
            val encrypted = ltd.evilcorp.atox.infrastructure.security.BiometricStorage.getEncryptedPassword(context) ?: return null
            val decryptedBytes = cipher.doFinal(encrypted)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Failed to decrypt password: $e")
            null
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
