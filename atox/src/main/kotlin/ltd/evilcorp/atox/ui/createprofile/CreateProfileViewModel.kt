// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.createprofile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import ltd.evilcorp.atox.infrastructure.tox.ToxStarter
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.auth.UserManager
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import ltd.evilcorp.domain.features.backup.usecase.BackupUseCase
import ltd.evilcorp.domain.features.auth.repository.IProfileRepository

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R

sealed interface CreateProfileError {
    object RestorePasswordRequired : CreateProfileError
    object RestoreFailed : CreateProfileError
    object BadProxyHost : CreateProfileError
    object BadProxyPort : CreateProfileError
    object BadProxyType : CreateProfileError
    object ProxyNotFound : CreateProfileError
    object Unknown : CreateProfileError
}

sealed interface CreateProfileUiState {
    object Idle : CreateProfileUiState
    object Loading : CreateProfileUiState
    object Success : CreateProfileUiState
    data class Error(val error: CreateProfileError) : CreateProfileUiState
}

@HiltViewModel
class CreateProfileViewModel @Inject constructor(
    private val backupProcessor: ProfileBackupProcessor,
    private val profileDeleter: IProfileRepository,
    private val backupUseCase: BackupUseCase,
    private val userManager: UserManager,
    private val tox: ITox,
    private val toxStarter: ToxStarter,
) : ViewModel() {
    private val _uiState = MutableStateFlow<CreateProfileUiState>(CreateProfileUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun startTox(save: ByteArray? = null, password: String? = null): ToxSaveStatus = toxStarter.startTox(save, password)
    suspend fun create(user: User) = userManager.create(user)

    fun restoreBackup(uriString: String, password: String?) {
        viewModelScope.launch {
            _uiState.value = CreateProfileUiState.Loading
            val status = withContext(Dispatchers.IO) {
                val backup = backupProcessor.readBackupBytes(uriString) ?: return@withContext ToxSaveStatus.SaveNotFound
                val toxCore = runCatching {
                    backupUseCase.providerData(backup, password, "tox_core")
                }.getOrNull() ?: return@withContext ToxSaveStatus.Encrypted
                profileDeleter.clearDatabase()
                val status = startTox(toxCore, password.takeIf { !it.isNullOrBlank() })
                if (status != ToxSaveStatus.Ok) return@withContext status
                runCatching {
                    backupUseCase.import(backup, password, skipIds = setOf("tox_core"))
                    userManager.verifyExists(tox.publicKey)
                }.onFailure {
                    return@withContext ToxSaveStatus.BadFormat
                }
                ToxSaveStatus.Ok
            }

            if (status == ToxSaveStatus.Ok) {
                _uiState.value = CreateProfileUiState.Success
            } else {
                val error = when (status) {
                    ToxSaveStatus.Encrypted -> CreateProfileError.RestorePasswordRequired
                    ToxSaveStatus.BadFormat -> CreateProfileError.RestoreFailed
                    else -> CreateProfileError.RestoreFailed
                }
                _uiState.value = CreateProfileUiState.Error(error)
            }
        }
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            _uiState.value = CreateProfileUiState.Loading
            val status = withContext(Dispatchers.IO) {
                val status = startTox()
                if (status == ToxSaveStatus.Ok) {
                    create(User(publicKey = tox.publicKey.string(), name = name))
                }
                status
            }

            if (status == ToxSaveStatus.Ok) {
                _uiState.value = CreateProfileUiState.Success
            } else {
                val error = when (status) {
                    ToxSaveStatus.BadProxyHost -> CreateProfileError.BadProxyHost
                    ToxSaveStatus.BadProxyPort -> CreateProfileError.BadProxyPort
                    ToxSaveStatus.BadProxyType -> CreateProfileError.BadProxyType
                    ToxSaveStatus.ProxyNotFound -> CreateProfileError.ProxyNotFound
                    else -> CreateProfileError.Unknown
                }
                _uiState.value = CreateProfileUiState.Error(error)
            }
        }
    }
}
