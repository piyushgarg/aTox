package ltd.evilcorp.atox.ui.navigation

import androidx.lifecycle.ViewModel
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import ltd.evilcorp.atox.infrastructure.tox.ToxStarter
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.settings.usecase.GetToxRunningStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import ltd.evilcorp.domain.core.di.IoDispatcher
import ltd.evilcorp.domain.features.auth.usecase.GetSelfAvatarUseCase
import ltd.evilcorp.domain.features.auth.usecase.ProfileRegistryUseCase
import ltd.evilcorp.domain.features.auth.usecase.GetSelfNameUseCase
import ltd.evilcorp.domain.features.auth.model.ProfileInfo

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

private const val LOAD_TIMEOUT_MS = 10_000L

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val getSelfUserUseCase: GetSelfUserUseCase,
    private val getToxRunningStateUseCase: GetToxRunningStateUseCase,
    private val toxStarter: ToxStarter,
    private val getSelfAvatarUseCase: GetSelfAvatarUseCase,
    private val profileRegistryUseCase: ProfileRegistryUseCase,
    private val getSelfNameUseCase: GetSelfNameUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val publicKey by lazy { getSelfUserUseCase.publicKey }

    val launchState = MutableStateFlow<LaunchUiState>(LaunchUiState.Loading)
    val unlockState = MutableStateFlow<UnlockUiState>(UnlockUiState.Idle)

    fun isToxRunning(): Boolean = getToxRunningStateUseCase.execute()

    fun tryLoadTox(password: String?): ToxSaveStatus {
        return toxStarter.tryLoadTox(password)
    }

    suspend fun loadToxAsync(password: String?) {
        if (getToxRunningStateUseCase.execute()) {
            launchState.value = LaunchUiState.Success(ToxSaveStatus.Ok)
            return
        }
        launchState.value = LaunchUiState.Loading
        try {
            withTimeout(LOAD_TIMEOUT_MS) {
                val result = withContext(ioDispatcher) {
                    toxStarter.tryLoadTox(password)
                }
                if (result == ToxSaveStatus.Ok) {
                    syncProfile()
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
        val success = withContext(ioDispatcher) {
            toxStarter.tryLoadTox(password) == ToxSaveStatus.Ok
        }
        if (success) {
            syncProfile()
            unlockState.value = UnlockUiState.Success
            return true
        } else {
            unlockState.value = UnlockUiState.Error
            return false
        }
    }

    private fun syncProfile() {
        val activeId = profileRegistryUseCase.getActiveProfileId()
        val name = getSelfNameUseCase.execute().takeIf { it.isNotBlank() } ?: "My Profile"
        val avatarFile = getSelfAvatarUseCase.execute()
        val avatarUri = if (avatarFile.exists() && avatarFile.length() > 0L) avatarFile.toURI().toString() else null
        
        val profiles = profileRegistryUseCase.getProfiles()
        val existing = profiles.find { it.id == activeId }
        val updatedProfile = if (existing != null) {
            existing.copy(name = name, avatarUri = avatarUri)
        } else {
            ProfileInfo(id = activeId, name = name, avatarUri = avatarUri)
        }
        profileRegistryUseCase.addOrUpdateProfile(updatedProfile)
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
