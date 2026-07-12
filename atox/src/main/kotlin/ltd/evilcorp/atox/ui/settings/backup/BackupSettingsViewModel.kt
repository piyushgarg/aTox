// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import ltd.evilcorp.domain.core.di.IoDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider
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
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val getUserSettingsUseCase: ltd.evilcorp.domain.features.settings.usecase.GetUserSettingsUseCase,
    private val fileProcessor: ISettingsFileProcessor,
    private val startToxUseCase: StartToxUseCase,
    private val manageToxLifecycleUseCase: ManageToxLifecycleUseCase,
    private val getToxRunningStateUseCase: GetToxRunningStateUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val getBackupProviderDataUseCase: GetBackupProviderDataUseCase,
    val backupProviders: List<@JvmSuppressWildcards IBackupDataProvider>,
    private val clearDatabaseUseCase: ClearDatabaseUseCase,
    private val getSelfUserUseCase: GetSelfUserUseCase,
    private val verifyProfileExistsUseCase: VerifyProfileExistsUseCase,
    private val manageProfileCheckpointUseCase: ManageProfileCheckpointUseCase,
    private val getCloudBackupsUseCase: ltd.evilcorp.domain.features.backup.usecase.GetCloudBackupsUseCase,
    private val downloadCloudBackupUseCase: ltd.evilcorp.domain.features.backup.usecase.DownloadCloudBackupUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _backupExporting = MutableStateFlow(false)
    val backupExporting: StateFlow<Boolean> get() = _backupExporting

    private val _googleBackups = MutableStateFlow<List<ltd.evilcorp.domain.features.backup.model.CloudBackupInfo>>(emptyList())
    val googleBackups: StateFlow<List<ltd.evilcorp.domain.features.backup.model.CloudBackupInfo>> get() = _googleBackups

    private val _backupImporting = MutableStateFlow(false)
    val backupImporting: StateFlow<Boolean> get() = _backupImporting

    private val _uiEvents = MutableSharedFlow<BackupUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    private var backupTimeoutJob: Job? = null

    private fun startBackupTimeout() {
        backupTimeoutJob?.cancel()
        backupTimeoutJob = viewModelScope.launch {
            delay(15_000L)
            if (_backupExporting.value) {
                val workManager = androidx.work.WorkManager.getInstance(context)
                workManager.cancelUniqueWork("ManualLocalBackup")
                workManager.cancelUniqueWork("ManualGoogleBackup")
                _backupExporting.value = false
                _uiEvents.emit(BackupUiEvent.ShowToast(R.string.backup_export_timeout))
            }
        }
    }

    init {
        viewModelScope.launch {
            val workManager = androidx.work.WorkManager.getInstance(context)
            val localFlow = workManager
                .getWorkInfosForUniqueWorkLiveData("ManualLocalBackup")
                .asFlow()
            val googleFlow = workManager
                .getWorkInfosForUniqueWorkLiveData("ManualGoogleBackup")
                .asFlow()
            
            combine(localFlow, googleFlow) { localInfos, googleInfos ->
                val localRunning = localInfos?.any {
                    it.state == androidx.work.WorkInfo.State.RUNNING ||
                        it.state == androidx.work.WorkInfo.State.ENQUEUED
                } == true
                val googleRunning = googleInfos?.any {
                    it.state == androidx.work.WorkInfo.State.RUNNING ||
                        it.state == androidx.work.WorkInfo.State.ENQUEUED
                } == true
                localRunning || googleRunning
            }.collect { isRunning ->
                _backupExporting.value = isRunning
            }
        }
    }

    fun isToxStarted(): Boolean = getToxRunningStateUseCase.execute()

    fun listGoogleDriveBackups() {
        viewModelScope.launch {
            val backups = withContext(ioDispatcher) { getCloudBackupsUseCase.execute() }
            _googleBackups.value = backups
        }
    }

    fun restoreGoogleDriveBackup(fileId: String, password: String?) {
        viewModelScope.launch {
            _backupImporting.value = true
            val success = withContext(ioDispatcher) {
                val checkpointCreated = manageProfileCheckpointUseCase.execute(CheckpointAction.Create)
                runCatching {
                    val backup = downloadCloudBackupUseCase.execute(fileId)
                    val toxCore = getBackupProviderDataUseCase.execute(backup, "tox_core") ?: error("Missing Tox core data")
                    manageToxLifecycleUseCase.execute(ToxLifecycleAction.Stop)
                    clearDatabaseUseCase.execute()
                    val status = startToxUseCase.execute(toxCore, password.takeIf { !it.isNullOrBlank() })
                    check(status == ToxSaveStatus.Ok) { "Unable to start restored profile: $status" }
                    importBackupUseCase.execute(backup, skipIds = setOf("tox_core"))
                    verifyProfileExistsUseCase.execute(getSelfUserUseCase.publicKey)
                    if (checkpointCreated) {
                        manageProfileCheckpointUseCase.execute(CheckpointAction.Clear)
                    }
                    true
                }.getOrElse { throwable ->
                    android.util.Log.e("BackupSettingsViewModel", "Google Drive Backup restore failed", throwable)
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

    fun createBackup() {
        viewModelScope.launch {
            _backupExporting.value = true
            withContext(ioDispatcher) {
                val workManager = androidx.work.WorkManager.getInstance(context)
                val localWork = androidx.work.OneTimeWorkRequestBuilder<
                    ltd.evilcorp.atox.infrastructure.backup.LocalSyncWorker
                >().build()
                workManager.enqueueUniqueWork(
                    "ManualLocalBackup",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    localWork
                )

                val settings = getUserSettingsUseCase.settings.value
                if (settings.backupGoogleAccount.isNotEmpty()) {
                    val googleWork = androidx.work.OneTimeWorkRequestBuilder<
                        ltd.evilcorp.atox.infrastructure.backup.google.GoogleDriveSyncWorker
                    >().build()
                    workManager.enqueueUniqueWork(
                        "ManualGoogleBackup",
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        googleWork
                    )
                }
            }
            _uiEvents.emit(BackupUiEvent.ShowToast(R.string.backup_creating))
            startBackupTimeout()
        }
    }

    fun createGoogleBackup() {
        viewModelScope.launch {
            _backupExporting.value = true
            withContext(ioDispatcher) {
                val workManager = androidx.work.WorkManager.getInstance(context)
                val googleWork = androidx.work.OneTimeWorkRequestBuilder<
                    ltd.evilcorp.atox.infrastructure.backup.google.GoogleDriveSyncWorker
                >().build()
                workManager.enqueueUniqueWork(
                    "ManualGoogleBackup",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    googleWork
                )
            }
            _uiEvents.emit(BackupUiEvent.ShowToast(R.string.backup_creating))
            startBackupTimeout()
        }
    }

    fun restoreBackup(uriString: String, password: String?) {
        viewModelScope.launch {
            _backupImporting.value = true
            val success = withContext(ioDispatcher) {
                val checkpointCreated = manageProfileCheckpointUseCase.execute(CheckpointAction.Create)
                runCatching {
                    val backup = fileProcessor.readBytes(uriString) ?: error("Unable to open backup")
                    val toxCore = getBackupProviderDataUseCase.execute(backup, "tox_core") ?: error("Missing Tox core data")
                    manageToxLifecycleUseCase.execute(ToxLifecycleAction.Stop)
                    clearDatabaseUseCase.execute()
                    val status = startToxUseCase.execute(toxCore, password.takeIf { !it.isNullOrBlank() })
                    check(status == ToxSaveStatus.Ok) { "Unable to start restored profile: $status" }
                    importBackupUseCase.execute(backup, skipIds = setOf("tox_core"))
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
