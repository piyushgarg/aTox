// SPDX-FileCopyrightText: 2019-2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.appearance.AppearanceManager
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.tox.ToxStarter
import ltd.evilcorp.core.model.BootstrapNodeSource
import ltd.evilcorp.core.model.FtAutoAccept
import ltd.evilcorp.core.model.DateFormatPreference
import ltd.evilcorp.core.model.TimeFormatPreference
import ltd.evilcorp.core.model.BackupFrequency
import ltd.evilcorp.core.model.BackupDestination
import ltd.evilcorp.core.tox.bootstrap.BootstrapNodeRegistry
import ltd.evilcorp.core.tox.save.ProxyType
import ltd.evilcorp.core.tox.save.SaveOptions
import ltd.evilcorp.core.tox.save.ToxSaveStatus
import ltd.evilcorp.core.tox.save.testToxSave
import ltd.evilcorp.domain.tox.Tox
import ltd.evilcorp.domain.feature.FileTransferManager
import ltd.evilcorp.domain.feature.UserManager
import ltd.evilcorp.domain.backup.BackupDataProvider
import ltd.evilcorp.domain.backup.BackupUseCase
import ltd.evilcorp.core.db.Database
import ltd.evilcorp.domain.usecase.CheckProxyUseCase
import ltd.evilcorp.domain.usecase.ProxyStatus
import ltd.evilcorp.domain.usecase.ImportBootstrapNodesUseCase

private const val TOX_SHUTDOWN_POLL_DELAY_MS = 200L

sealed interface SettingsUiEvent {
    data class ShowToast(val messageResId: Int) : SettingsUiEvent
}

class SettingsViewModel @Inject constructor(
    private val settings: Settings,
    private val appearanceManager: AppearanceManager,
    private val toxStarter: ToxStarter,
    private val tox: Tox,
    private val nodeRegistry: BootstrapNodeRegistry,
    private val fileTransferManager: FileTransferManager,
    private val checkProxyUseCase: CheckProxyUseCase,
    private val importBootstrapNodesUseCase: ImportBootstrapNodesUseCase,
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

    fun setShowProxyDialog(show: Boolean) {
        _showProxyDialog.value = show
    }

    fun setShowFtAcceptDialog(show: Boolean) {
        _showFtAcceptDialog.value = show
    }

    fun setShowBootstrapDialog(show: Boolean) {
        _showBootstrapDialog.value = show
    }

    fun isToxStarted(): Boolean = tox.started

    private val _uiEvents = MutableSharedFlow<SettingsUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    fun nospamAvailable(): Boolean = tox.started
    fun getNospam(): Int = tox.nospam
    fun setNospam(value: Int) {
        tox.nospam = value
    }

    // The trickery here is because the values in the dropdown are 0, 1, 2 for auto, no, yes;
    // while in Android, the values are -1, 1, 2 for auto, no, yes; so we map -1 to 0 when getting,
    // and 0 to -1 when setting.
    fun getTheme(): Int = max(0, appearanceManager.appearance.value.themeMode)
    fun setTheme(theme: Int) {
        appearanceManager.updateThemeMode(
            when (theme) {
                0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                1 -> AppCompatDelegate.MODE_NIGHT_NO
                2 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    fun getFtAutoAccept(): FtAutoAccept = settings.ftAutoAccept
    fun setFtAutoAccept(autoAccept: FtAutoAccept) {
        settings.ftAutoAccept = autoAccept
    }

    fun getUdpEnabled(): Boolean = settings.udpEnabled
    fun setUdpEnabled(enabled: Boolean) {
        if (enabled == getUdpEnabled()) return
        settings.udpEnabled = enabled
        restartNeeded = true
    }

    fun getRunAtStartup(): Boolean = settings.runAtStartup
    fun setRunAtStartup(enabled: Boolean) {
        settings.runAtStartup = enabled
    }

    fun getAutoAwayEnabled() = settings.autoAwayEnabled
    fun setAutoAwayEnabled(enabled: Boolean) {
        settings.autoAwayEnabled = enabled
    }

    fun getConfirmQuitting(): Boolean = settings.confirmQuitting
    fun setConfirmQuitting(enabled: Boolean) {
        settings.confirmQuitting = enabled
    }

    fun getConfirmCalling(): Boolean = settings.confirmCalling
    fun setConfirmCalling(enabled: Boolean) {
        settings.confirmCalling = enabled
    }

    fun getAutoAwaySeconds() = settings.autoAwaySeconds
    fun setAutoAwaySeconds(seconds: Long) {
        settings.autoAwaySeconds = seconds
    }

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

    private var checkProxyJob: Job? = null
    fun checkProxy() {
        checkProxyJob?.cancel(null)
        checkProxyJob = viewModelScope.launch(Dispatchers.IO) {
            val proxyStatusResult = checkProxyUseCase.execute(
                getUdpEnabled(),
                getProxyType(),
                getProxyAddress(),
                getProxyPort()
            )
            _proxyStatus.value = proxyStatusResult
        }
    }

    fun getProxyType(): ProxyType = settings.proxyType
    fun setProxyType(type: ProxyType) {
        if (type != getProxyType()) {
            settings.proxyType = type
            restartNeeded = true
            checkProxy()
        }
    }

    fun getProxyAddress(): String = settings.proxyAddress
    fun setProxyAddress(address: String) {
        if (address != getProxyAddress()) {
            settings.proxyAddress = address
            if (getProxyType() != ProxyType.None) {
                restartNeeded = true
            }
            checkProxy()
        }
    }

    fun getProxyPort(): Int = settings.proxyPort
    fun setProxyPort(port: Int) {
        if (port != getProxyPort()) {
            settings.proxyPort = port
            if (getProxyType() != ProxyType.None) {
                restartNeeded = true
            }
            checkProxy()
        }
    }

    fun setProxyPortString(portStr: String): Boolean {
        if (portStr.isEmpty()) {
            setProxyPort(0)
            return true
        }
        if (portStr.all { it.isDigit() }) {
            val portInt = portStr.toIntOrNull()
            if (portInt != null && portInt in 0..65535) {
                setProxyPort(portInt)
                return true
            }
        }
        return false
    }

    fun isCurrentPassword(maybeCurrentPassword: String) = tox.password == maybeCurrentPassword.ifEmpty { null }

    fun setPassword(newPassword: String) = tox.changePassword(newPassword.ifEmpty { null })

    fun getBootstrapNodeSource(): BootstrapNodeSource = settings.bootstrapNodeSource
    fun setBootstrapNodeSource(source: BootstrapNodeSource) {
        settings.bootstrapNodeSource = source
        nodeRegistry.reset()
        restartNeeded = true
    }

    suspend fun validateNodeJson(uriString: String): Boolean {
        return importBootstrapNodesUseCase.validate(uriString)
    }

    suspend fun importNodeJson(uriString: String): Boolean {
        return importBootstrapNodesUseCase.import(uriString)
    }

    fun getDisableScreenshots(): Boolean = settings.disableScreenshots
    fun setDisableScreenshots(disable: Boolean) {
        settings.disableScreenshots = disable
    }

    fun getCacheSize(): Long = fileTransferManager.getCacheSize()
    fun clearCache() = fileTransferManager.clearCache()

    fun setHapticEnabled(enabled: Boolean) {
        settings.hapticEnabled = enabled
    }

    fun setDateFormatPreference(pref: DateFormatPreference) {
        settings.dateFormatPreference = pref
    }

    fun setTimeFormatPreference(pref: TimeFormatPreference) {
        settings.timeFormatPreference = pref
    }

    fun setAutoSaveToDownloads(enabled: Boolean) {
        settings.autoSaveToDownloads = enabled
    }

    fun setEnableReplies(enabled: Boolean) {
        settings.enableReplies = enabled
    }

    fun setAutoSaveDirectoryUri(uri: String) {
        settings.autoSaveDirectoryUri = uri
    }

    fun setSentMessageSoundVolume(volume: Int) {
        settings.sentMessageSoundVolume = volume
    }

    fun setCallSoundVolume(volume: Int) {
        settings.callSoundVolume = volume
    }

    fun setNotificationSoundVolume(volume: Int) {
        settings.notificationSoundVolume = volume
    }

    fun setActiveChatSoundVolume(volume: Int) {
        settings.activeChatSoundVolume = volume
    }

    fun setSentMessageSoundUri(uri: String) {
        settings.sentMessageSoundUri = uri
    }

    fun setCallRingtoneUri(uri: String) {
        settings.callRingtoneUri = uri
    }

    fun setNotificationSoundUri(uri: String) {
        settings.notificationSoundUri = uri
    }

    fun setActiveChatSoundUri(uri: String) {
        settings.activeChatSoundUri = uri
    }

    fun setBackupEncryptionEnabled(enabled: Boolean) {
        settings.backupEncryptionEnabled = enabled
    }

    fun setBackupGoogleAccount(account: String) {
        settings.backupGoogleAccount = account
    }

    fun setAutomaticBackupEnabled(enabled: Boolean) {
        settings.automaticBackupEnabled = enabled
    }

    fun setBackupFrequency(frequency: BackupFrequency) {
        settings.backupFrequency = frequency
    }

    fun setBackupUseCellular(useCellular: Boolean) {
        settings.backupUseCellular = useCellular
    }

    fun setBackupDestinations(destinations: Set<BackupDestination>) {
        settings.backupDestinations = destinations
    }

    fun setBackupEndToEndEncryptionEnabled(enabled: Boolean) {
        settings.backupEndToEndEncryptionEnabled = enabled
    }


}
