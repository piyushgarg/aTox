// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.createprofile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import ltd.evilcorp.domain.core.di.IoDispatcher
import ltd.evilcorp.atox.infrastructure.tox.ToxStarter
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import ltd.evilcorp.domain.features.backup.usecase.GetBackupProviderDataUseCase
import ltd.evilcorp.domain.features.backup.usecase.ImportBackupUseCase
import ltd.evilcorp.domain.features.auth.usecase.ClearDatabaseUseCase
import ltd.evilcorp.domain.features.auth.usecase.CreateProfileUseCase
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.auth.usecase.VerifyProfileExistsUseCase

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.features.auth.usecase.ProfileRegistryUseCase

sealed interface CreateProfileUiState {
    object Idle : CreateProfileUiState
    object Loading : CreateProfileUiState
    object Success : CreateProfileUiState
    data class Error(val errorResId: Int) : CreateProfileUiState
}

@HiltViewModel
class CreateProfileViewModel @Inject constructor(
    private val backupProcessor: ProfileBackupProcessor,
    private val clearDatabaseUseCase: ClearDatabaseUseCase,
    private val getBackupProviderDataUseCase: GetBackupProviderDataUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
    private val getSelfUserUseCase: GetSelfUserUseCase,
    private val verifyProfileExistsUseCase: VerifyProfileExistsUseCase,
    private val toxStarter: ToxStarter,
    private val getCloudBackupsUseCase: ltd.evilcorp.domain.features.backup.usecase.GetCloudBackupsUseCase,
    private val downloadCloudBackupUseCase: ltd.evilcorp.domain.features.backup.usecase.DownloadCloudBackupUseCase,
    private val tox: ltd.evilcorp.domain.core.network.ITox,
    private val profileRegistryUseCase: ProfileRegistryUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow<CreateProfileUiState>(CreateProfileUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _googleBackups = MutableStateFlow<List<ltd.evilcorp.domain.features.backup.model.CloudBackupInfo>>(emptyList())
    val googleBackups: StateFlow<List<ltd.evilcorp.domain.features.backup.model.CloudBackupInfo>> get() = _googleBackups

    fun startTox(save: ByteArray? = null, password: String? = null): ToxSaveStatus = toxStarter.startTox(save, password)
    suspend fun create(user: User) = createProfileUseCase.execute(user)

    fun restoreBackup(uriString: String, password: String?) {
        viewModelScope.launch {
            _uiState.value = CreateProfileUiState.Loading
            val status = withContext(ioDispatcher) {
                val backup = backupProcessor.readBackupBytes(uriString) ?: return@withContext ToxSaveStatus.SaveNotFound
                val toxCore = runCatching {
                    getBackupProviderDataUseCase.execute(backup, "tox_core")
                }.getOrNull() ?: return@withContext ToxSaveStatus.Encrypted
                clearDatabaseUseCase.execute()
                val status = startTox(toxCore, password.takeIf { !it.isNullOrBlank() })
                if (status != ToxSaveStatus.Ok) return@withContext status
                runCatching {
                    importBackupUseCase.execute(backup, skipIds = setOf("tox_core"))
                    verifyProfileExistsUseCase.execute(getSelfUserUseCase.publicKey)
                }.onFailure {
                    return@withContext ToxSaveStatus.BadFormat
                }
                ToxSaveStatus.Ok
            }

            if (status == ToxSaveStatus.Ok) {
                val name = tox.getName()
                finalizeProfileCreation(name)
                _uiState.value = CreateProfileUiState.Success
            } else {
                val errorResId = when (status) {
                    ToxSaveStatus.Encrypted -> R.string.backup_import_password_required
                    else -> R.string.backup_import_failure
                }
                _uiState.value = CreateProfileUiState.Error(errorResId)
            }
        }
    }

    fun listGoogleDriveBackups() {
        viewModelScope.launch {
            val backups = getCloudBackupsUseCase.execute()
            _googleBackups.value = backups
        }
    }

    fun restoreGoogleDriveBackup(fileId: String, password: String?) {
        viewModelScope.launch {
            _uiState.value = CreateProfileUiState.Loading
            val status = withContext(ioDispatcher) {
                val backup = downloadCloudBackupUseCase.execute(fileId)
                val toxCore = runCatching {
                    getBackupProviderDataUseCase.execute(backup, "tox_core")
                }.getOrNull() ?: return@withContext ToxSaveStatus.Encrypted
                clearDatabaseUseCase.execute()
                val status = startTox(toxCore, password.takeIf { !it.isNullOrBlank() })
                if (status != ToxSaveStatus.Ok) return@withContext status
                runCatching {
                    importBackupUseCase.execute(backup, skipIds = setOf("tox_core"))
                    verifyProfileExistsUseCase.execute(getSelfUserUseCase.publicKey)
                }.onFailure {
                    return@withContext ToxSaveStatus.BadFormat
                }
                ToxSaveStatus.Ok
            }

            if (status == ToxSaveStatus.Ok) {
                val name = tox.getName()
                finalizeProfileCreation(name)
                _uiState.value = CreateProfileUiState.Success
            } else {
                val errorResId = when (status) {
                    ToxSaveStatus.Encrypted -> R.string.backup_import_password_required
                    else -> R.string.backup_import_failure
                }
                _uiState.value = CreateProfileUiState.Error(errorResId)
            }
        }
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            _uiState.value = CreateProfileUiState.Loading
            val status = withContext(ioDispatcher) {
                val status = startTox()
                if (status == ToxSaveStatus.Ok) {
                    create(User(publicKey = getSelfUserUseCase.publicKey.string(), name = name))
                }
                status
            }

            if (status == ToxSaveStatus.Ok) {
                finalizeProfileCreation(name)
                _uiState.value = CreateProfileUiState.Success
            } else {
                val errorResId = when (status) {
                    ToxSaveStatus.BadProxyHost -> R.string.bad_host
                    ToxSaveStatus.BadProxyPort -> R.string.bad_port
                    ToxSaveStatus.BadProxyType -> R.string.bad_type
                    ToxSaveStatus.ProxyNotFound -> R.string.proxy_not_found
                    else -> R.string.create_profile_error_failed
                }
                _uiState.value = CreateProfileUiState.Error(errorResId)
            }
        }
    }

    private suspend fun finalizeProfileCreation(name: String) = withContext(ioDispatcher) {
        val oldId = profileRegistryUseCase.getActiveProfileId()
        val newId = getSelfUserUseCase.publicKey.string()
        profileRegistryUseCase.finalizeProfileCreation(oldId, newId, name)
    }
}
