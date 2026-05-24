// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.tox.ToxStarter
import ltd.evilcorp.core.db.Database
import ltd.evilcorp.core.tox.save.ToxSaveStatus
import ltd.evilcorp.domain.backup.BackupDataProvider
import ltd.evilcorp.domain.backup.BackupUseCase
import ltd.evilcorp.domain.feature.UserManager
import ltd.evilcorp.domain.tox.Tox

sealed interface BackupUiEvent {
    data class ShowToast(val messageResId: Int) : BackupUiEvent
}

class BackupSettingsViewModel @Inject constructor(
    private val fileProcessor: SettingsFileProcessor,
    private val toxStarter: ToxStarter,
    private val tox: Tox,
    private val backupUseCase: BackupUseCase,
    private val database: Database,
    private val userManager: UserManager,
) : ViewModel() {
    private val _backupExporting = MutableStateFlow(false)
    val backupExporting: StateFlow<Boolean> get() = _backupExporting

    private val _backupImporting = MutableStateFlow(false)
    val backupImporting: StateFlow<Boolean> get() = _backupImporting

    private val _uiEvents = MutableSharedFlow<BackupUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    val backupProviders: List<BackupDataProvider> = backupUseCase.providers

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
                runCatching {
                    val backup = fileProcessor.readBytes(uriString) ?: error("Unable to open backup")
                    val toxCore = backupUseCase.providerData(backup, password, "tox_core") ?: error("Missing Tox core data")
                    toxStarter.stopTox()
                    database.clearAllTables()
                    val status = toxStarter.startTox(toxCore, password.takeIf { !it.isNullOrBlank() })
                    check(status == ToxSaveStatus.Ok) { "Unable to start restored profile: $status" }
                    backupUseCase.import(backup, password, skipIds = setOf("tox_core"))
                    userManager.verifyExists(tox.publicKey)
                    true
                }.getOrElse { false }
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
