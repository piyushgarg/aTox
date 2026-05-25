// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.createprofile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.atox.tox.ToxStarter
import ltd.evilcorp.core.db.Database
import ltd.evilcorp.core.model.User
import ltd.evilcorp.domain.feature.UserManager
import ltd.evilcorp.domain.tox.Tox
import ltd.evilcorp.core.tox.save.ToxSaveStatus
import ltd.evilcorp.domain.backup.BackupUseCase
import ltd.evilcorp.domain.model.toDomain

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.R

sealed interface CreateProfileUiState {
    object Idle : CreateProfileUiState
    object Loading : CreateProfileUiState
    object Success : CreateProfileUiState
    data class Error(val errorResId: Int) : CreateProfileUiState
}

class CreateProfileViewModel @Inject constructor(
    private val backupProcessor: ProfileBackupProcessor,
    private val database: Database,
    private val backupUseCase: BackupUseCase,
    private val userManager: UserManager,
    private val tox: Tox,
    private val toxStarter: ToxStarter,
) : ViewModel() {
    private val _uiState = MutableStateFlow<CreateProfileUiState>(CreateProfileUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun startTox(save: ByteArray? = null, password: String? = null): ToxSaveStatus = toxStarter.startTox(save, password)
    fun create(user: User) = userManager.create(user.toDomain())

    fun restoreBackup(uriString: String, password: String?) {
        viewModelScope.launch {
            _uiState.value = CreateProfileUiState.Loading
            val status = withContext(Dispatchers.IO) {
                val backup = backupProcessor.readBackupBytes(uriString) ?: return@withContext ToxSaveStatus.SaveNotFound
                val toxCore = runCatching {
                    backupUseCase.providerData(backup, password, "tox_core")
                }.getOrNull() ?: return@withContext ToxSaveStatus.Encrypted
                database.clearAllTables()
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
                val errorResId = when (status) {
                    ToxSaveStatus.Encrypted -> R.string.backup_import_password_required
                    ToxSaveStatus.BadFormat -> R.string.backup_import_failure
                    else -> R.string.backup_import_failure
                }
                _uiState.value = CreateProfileUiState.Error(errorResId)
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
}
