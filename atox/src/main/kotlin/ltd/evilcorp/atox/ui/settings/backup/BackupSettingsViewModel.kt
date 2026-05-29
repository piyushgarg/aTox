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
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider
import ltd.evilcorp.domain.features.backup.usecase.ExportBackupUseCase
import ltd.evilcorp.domain.features.backup.usecase.ImportBackupUseCase
import ltd.evilcorp.domain.features.backup.usecase.GetBackupProviderDataUseCase
import ltd.evilcorp.domain.features.settings.ISettingsFileProcessor
import ltd.evilcorp.domain.features.settings.usecase.GetToxRunningStateUseCase
import ltd.evilcorp.domain.features.settings.usecase.StartToxUseCase
import ltd.evilcorp.domain.features.settings.usecase.ManageToxLifecycleUseCase
import ltd.evilcorp.domain.features.settings.usecase.ToxLifecycleAction
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase
import ltd.evilcorp.domain.features.auth.usecase.VerifyProfileExistsUseCase
import ltd.evilcorp.domain.features.auth.usecase.ClearDatabaseUseCase
import ltd.evilcorp.domain.features.auth.usecase.ManageProfileCheckpointUseCase
import ltd.evilcorp.domain.features.auth.usecase.CheckpointAction

sealed interface BackupUiEvent {
    data class ShowToast(val messageResId: Int) : BackupUiEvent
}

@HiltViewModel
class BackupSettingsViewModel @Inject constructor(
    private val fileProcessor: ISettingsFileProcessor,
    private val startToxUseCase: StartToxUseCase,
    private val manageToxLifecycleUseCase: ManageToxLifecycleUseCase,
    private val getToxRunningStateUseCase: GetToxRunningStateUseCase,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val getBackupProviderDataUseCase: GetBackupProviderDataUseCase,
    val backupProviders: List<@JvmSuppressWildcards IBackupDataProvider>,
    private val clearDatabaseUseCase: ClearDatabaseUseCase,
    private val getSelfUserUseCase: GetSelfUserUseCase,
    private val verifyProfileExistsUseCase: VerifyProfileExistsUseCase,
    private val manageProfileCheckpointUseCase: ManageProfileCheckpointUseCase,
) : ViewModel() {
    private val _backupExporting = MutableStateFlow(false)
    val backupExporting: StateFlow<Boolean> get() = _backupExporting

    private val _backupImporting = MutableStateFlow(false)
    val backupImporting: StateFlow<Boolean> get() = _backupImporting

    private val _uiEvents = MutableSharedFlow<BackupUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    fun isToxStarted(): Boolean = getToxRunningStateUseCase.execute()

    fun exportBackup(uriString: String, selectedIds: Set<String>, password: String?) {
        viewModelScope.launch {
            _backupExporting.value = true
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val data = exportBackupUseCase.execute(selectedIds, password)
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
                val checkpointCreated = manageProfileCheckpointUseCase.execute(CheckpointAction.Create)
                runCatching {
                    val backup = fileProcessor.readBytes(uriString) ?: error("Unable to open backup")
                    val toxCore = getBackupProviderDataUseCase.execute(backup, password, "tox_core") ?: error("Missing Tox core data")
                    manageToxLifecycleUseCase.execute(ToxLifecycleAction.Stop)
                    clearDatabaseUseCase.execute()
                    val status = startToxUseCase.execute(toxCore, password.takeIf { !it.isNullOrBlank() })
                    check(status == ToxSaveStatus.Ok) { "Unable to start restored profile: $status" }
                    importBackupUseCase.execute(backup, password, skipIds = setOf("tox_core"))
                    verifyProfileExistsUseCase.execute(getSelfUserUseCase.publicKey)
                    if (checkpointCreated) {
                        manageProfileCheckpointUseCase.execute(CheckpointAction.Clear)
                    }
                    true
                }.getOrElse { throwable ->
                    android.util.Log.e("BackupSettingsViewModel", "Backup restore failed", throwable)
                    if (checkpointCreated) {
                        manageProfileCheckpointUseCase.execute(CheckpointAction.Restore)
                    }
                    manageToxLifecycleUseCase.execute(ToxLifecycleAction.Stop)
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
