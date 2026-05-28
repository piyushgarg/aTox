// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.core.network.IToxStarter
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider
import ltd.evilcorp.domain.features.backup.usecase.BackupUseCase
import ltd.evilcorp.domain.features.auth.repository.IProfileRepository
import ltd.evilcorp.domain.features.auth.UserManager
import ltd.evilcorp.domain.core.network.ITox

import ltd.evilcorp.domain.features.settings.ISettingsFileProcessor

sealed interface BackupUiEvent {
    data class ShowToast(val messageResId: Int) : BackupUiEvent
}

@HiltViewModel
class BackupSettingsViewModel @Inject constructor(
    private val fileProcessor: ISettingsFileProcessor,
    private val toxStarter: IToxStarter,
    private val tox: ITox,
    private val backupUseCase: BackupUseCase,
    private val profileDeleter: IProfileRepository,
    private val userManager: UserManager,
) : ViewModel() {
    private val _backupExporting = MutableStateFlow(false)
    val backupExporting: StateFlow<Boolean> get() = _backupExporting

    private val _backupImporting = MutableStateFlow(false)
    val backupImporting: StateFlow<Boolean> get() = _backupImporting

    private val _uiEvents = MutableSharedFlow<BackupUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    val backupProviders: List<IBackupDataProvider> = backupUseCase.providers

    fun isToxStarted(): Boolean = tox.started

    fun exportBackup(uriString: String, selectedIds: Set<String>, password: String?) {
        viewModelScope.launch {
            _backupExporting.value = true
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val data = backupUseCase.export(selectedIds, password)
                    fileProcessor.writeBytes(uriString, data)
                }.getOrElse { false }
            }
            _backupExporting.value = false
            _uiEvents.emit(
                BackupUiEvent.ShowToast(
                    if (success) R.string.backup_export_success else R.string.backup_export_failure
                )
            )
        }
    }

    fun restoreBackup(uriString: String, password: String?) {
        viewModelScope.launch {
            _backupImporting.value = true
            val success = withContext(Dispatchers.IO) {
                val checkpointCreated = profileDeleter.createCheckpoint()
                runCatching {
                    val backup = fileProcessor.readBytes(uriString) ?: error("Unable to open backup")
                    val toxCore = backupUseCase.providerData(backup, password, "tox_core") ?: error("Missing Tox core data")
                    toxStarter.stopTox()
                    profileDeleter.clearDatabase()
                    val status = toxStarter.startTox(toxCore, password.takeIf { !it.isNullOrBlank() })
                    check(status == ToxSaveStatus.Ok) { "Unable to start restored profile: $status" }
                    backupUseCase.import(backup, password, skipIds = setOf("tox_core"))
                    userManager.verifyExists(tox.publicKey)
                    if (checkpointCreated) {
                        profileDeleter.clearCheckpoint()
                    }
                    true
                }.getOrElse { throwable ->
                    android.util.Log.e("BackupSettingsViewModel", "Backup restore failed", throwable)
                    if (checkpointCreated) {
                        profileDeleter.restoreFromCheckpoint()
                    }
                    toxStarter.stopTox()
                    false
                }
            }
            _backupImporting.value = false
            _uiEvents.emit(
                BackupUiEvent.ShowToast(
                    if (success) R.string.backup_import_success else R.string.backup_import_failure
                )
            )
        }
    }
}
