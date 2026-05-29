// SPDX-FileCopyrightText: 2019-2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.auth.model.User
import ltd.evilcorp.domain.features.settings.model.BackupDestination
import ltd.evilcorp.domain.features.settings.model.BackupFrequency
import ltd.evilcorp.domain.features.settings.model.BootstrapNodeSource
import ltd.evilcorp.domain.features.settings.model.ProxyStatus
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.features.settings.usecase.CheckProxyUseCase
import ltd.evilcorp.domain.features.settings.usecase.GetUserSettingsUseCase
import ltd.evilcorp.domain.features.settings.usecase.UpdateUserSettingsUseCase
import ltd.evilcorp.domain.features.settings.usecase.UpdateAction
import ltd.evilcorp.domain.features.settings.usecase.ManageToxLifecycleUseCase
import ltd.evilcorp.domain.features.settings.usecase.GetCacheSizeUseCase
import ltd.evilcorp.domain.features.settings.usecase.ClearCacheUseCase
import ltd.evilcorp.domain.features.settings.usecase.SetRunAtStartupUseCase
import ltd.evilcorp.domain.features.auth.usecase.GetSelfUserUseCase

private const val TOX_SHUTDOWN_POLL_DELAY_MS = 200L
private const val MAX_PORT_NUMBER = 65535

sealed interface SettingsUiEvent {
    data class ShowToast(val messageResId: Int) : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase,
    private val manageToxLifecycleUseCase: ManageToxLifecycleUseCase,
    private val getCacheSizeUseCase: GetCacheSizeUseCase,
    private val clearCacheUseCase: ClearCacheUseCase,
    private val setRunAtStartupUseCase: SetRunAtStartupUseCase,
    private val checkProxyUseCase: CheckProxyUseCase,
    private val getSelfUserUseCase: GetSelfUserUseCase,
) : ViewModel() {
    val publicKey by lazy { getSelfUserUseCase.publicKey }
    val user: StateFlow<User?> = getSelfUserUseCase.execute()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

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
        if (enabled == getUserSettingsUseCase.settings.value.udpEnabled) return
        viewModelScope.launch {
            updateUserSettingsUseCase.execute(UpdateAction.UdpEnabled(enabled))
        }
        restartNeeded = true
    }

    fun setRunAtStartup(enabled: Boolean) {
        viewModelScope.launch {
            setRunAtStartupUseCase.execute(enabled)
        }
    }

    fun setProxyPortString(portStr: String): Boolean {
        if (portStr.isEmpty()) {
            viewModelScope.launch {
                updateUserSettingsUseCase.execute(UpdateAction.ProxyPort(0))
            }
            if (getUserSettingsUseCase.settings.value.proxyType != ProxyType.None) {
                restartNeeded = true
            }
            checkProxy()
            return true
        }
        if (portStr.all { it.isDigit() }) {
            val portInt = portStr.toIntOrNull()
            if (portInt != null && portInt in 0..MAX_PORT_NUMBER) {
                viewModelScope.launch {
                    updateUserSettingsUseCase.execute(UpdateAction.ProxyPort(portInt))
                }
                if (getUserSettingsUseCase.settings.value.proxyType != ProxyType.None) {
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
            val currentSettings = getUserSettingsUseCase.settings.value
            val proxyStatusResult = checkProxyUseCase.execute(
                currentSettings.udpEnabled,
                currentSettings.proxyType,
                currentSettings.proxyAddress,
                currentSettings.proxyPort
            )
            _proxyStatus.value = proxyStatusResult
        }
    }

    fun setBootstrapNodeSource(source: BootstrapNodeSource) {
        viewModelScope.launch {
            updateUserSettingsUseCase.execute(UpdateAction.BootstrapNodeSourceAction(source))
        }
        restartNeeded = true
    }

    fun setBackupFrequency(frequency: BackupFrequency) {
        viewModelScope.launch {
            updateUserSettingsUseCase.execute(UpdateAction.BackupFrequencyAction(frequency))
        }
    }

    fun setBackupDestinations(destinations: Set<BackupDestination>) {
        viewModelScope.launch {
            updateUserSettingsUseCase.execute(
                UpdateAction.BackupDestinationOrdinals(
                    destinations.takeIf { it.isNotEmpty() }?.map { it.ordinal }?.toSet()
                        ?: setOf(BackupDestination.Local.ordinal)
                )
            )
        }
    }

    fun setSentMessageSoundUri(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            updateUserSettingsUseCase.execute(UpdateAction.SentMessageSoundUri(uri))
        }
    }

    fun setCallRingtoneUri(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            updateUserSettingsUseCase.execute(UpdateAction.CallRingtoneUri(uri))
        }
    }

    fun setNotificationSoundUri(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            updateUserSettingsUseCase.execute(UpdateAction.NotificationSoundUri(uri))
        }
    }

    fun setActiveChatSoundUri(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            updateUserSettingsUseCase.execute(UpdateAction.ActiveChatSoundUri(uri))
        }
    }

    fun setAutoSaveDirectoryUri(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            updateUserSettingsUseCase.execute(UpdateAction.AutoSaveDirectoryUri(uri))
        }
    }

    fun getCacheSize(): Long = getCacheSizeUseCase.execute()
    fun clearCache() = clearCacheUseCase.execute()

    fun commit() {
        if (!restartNeeded) {
            _committed.value = true
            return
        }

        val password = manageToxLifecycleUseCase.password
        viewModelScope.launch {
            manageToxLifecycleUseCase.execute(ltd.evilcorp.domain.features.settings.usecase.ToxLifecycleAction.Stop)
            while (manageToxLifecycleUseCase.started) {
                delay(TOX_SHUTDOWN_POLL_DELAY_MS)
            }
            manageToxLifecycleUseCase.execute(ltd.evilcorp.domain.features.settings.usecase.ToxLifecycleAction.TryLoad(password))
            _committed.value = true
        }
    }
}
