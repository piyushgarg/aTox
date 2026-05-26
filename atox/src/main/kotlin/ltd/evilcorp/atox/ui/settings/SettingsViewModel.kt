// SPDX-FileCopyrightText: 2019-2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.tox.ToxStarter
import ltd.evilcorp.domain.model.BootstrapNodeSource
import ltd.evilcorp.domain.model.BackupFrequency
import ltd.evilcorp.domain.model.BackupDestination
import ltd.evilcorp.core.tox.bootstrap.BootstrapNodeRegistry
import ltd.evilcorp.domain.model.ProxyType
import ltd.evilcorp.domain.tox.ITox
import ltd.evilcorp.domain.feature.FileTransferManager
import ltd.evilcorp.domain.usecase.CheckProxyUseCase
import ltd.evilcorp.domain.model.ProxyStatus

private const val TOX_SHUTDOWN_POLL_DELAY_MS = 200L

sealed interface SettingsUiEvent {
    data class ShowToast(val messageResId: Int) : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: Settings,
    private val toxStarter: ToxStarter,
    private val tox: ITox,
    private val nodeRegistry: BootstrapNodeRegistry,
    private val fileTransferManager: FileTransferManager,
    private val checkProxyUseCase: CheckProxyUseCase,
) : ViewModel() {
    private var restartNeeded = false

    private val _proxyStatus = MutableStateFlow<ProxyStatus?>(null)
    val proxyStatus: StateFlow<ProxyStatus?> get() = _proxyStatus

    private val _committed = MutableStateFlow(false)
    val committed: StateFlow<Boolean> get() = _committed

    private val _showProxyDialog = MutableStateFlow(false)
    val showProxyDialog: StateFlow<Boolean> get() = _showProxyDialog

    private val _showFtAcceptDialog = MutableStateFlow(false)
    val showFtAcceptDialog: StateFlow<Boolean> get() = _showFtAcceptDialog

    private val _showBootstrapDialog = MutableStateFlow(false)
    val showBootstrapDialog: StateFlow<Boolean> get() = _showBootstrapDialog

    private val _uiEvents = MutableSharedFlow<SettingsUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    fun setShowProxyDialog(show: Boolean) {
        _showProxyDialog.value = show
    }

    fun setShowFtAcceptDialog(show: Boolean) {
        _showFtAcceptDialog.value = show
    }

    fun setShowBootstrapDialog(show: Boolean) {
        _showBootstrapDialog.value = show
    }

    fun setUdpEnabled(enabled: Boolean) {
        if (enabled == settings.udpEnabled) return
        settings.udpEnabled = enabled
        restartNeeded = true
    }

    fun setRunAtStartup(enabled: Boolean) {
        settings.runAtStartup = enabled
    }

    fun setProxyPortString(portStr: String): Boolean {
        if (portStr.isEmpty()) {
            settings.proxyPort = 0
            if (settings.proxyType != ProxyType.None) {
                restartNeeded = true
            }
            checkProxy()
            return true
        }
        if (portStr.all { it.isDigit() }) {
            val portInt = portStr.toIntOrNull()
            if (portInt != null && portInt in 0..65535) {
                settings.proxyPort = portInt
                if (settings.proxyType != ProxyType.None) {
                    restartNeeded = true
                }
                checkProxy()
                return true
            }
        }
        return false
    }

    private var checkProxyJob: Job? = null
    fun checkProxy() {
        checkProxyJob?.cancel(null)
        checkProxyJob = viewModelScope.launch(Dispatchers.IO) {
            val proxyStatusResult = checkProxyUseCase.execute(
                settings.udpEnabled,
                settings.proxyType,
                settings.proxyAddress,
                settings.proxyPort
            )
            _proxyStatus.value = proxyStatusResult
        }
    }

    fun setBootstrapNodeSource(source: BootstrapNodeSource) {
        settings.bootstrapNodeSource = source
        nodeRegistry.reset()
        restartNeeded = true
    }

    fun setBackupFrequency(frequency: BackupFrequency) {
        settings.backupFrequency = frequency
    }

    fun setBackupDestinations(destinations: Set<BackupDestination>) {
        settings.backupDestinations = destinations
    }

    fun getCacheSize(): Long = fileTransferManager.getCacheSize()
    fun clearCache() = fileTransferManager.clearCache()

    fun commit() {
        if (!restartNeeded) {
            _committed.value = true
            return
        }

        val password = tox.password
        toxStarter.stopTox()

        viewModelScope.launch {
            while (tox.started) {
                delay(TOX_SHUTDOWN_POLL_DELAY_MS)
            }
            toxStarter.tryLoadTox(password)
            _committed.value = true
        }
    }
}
