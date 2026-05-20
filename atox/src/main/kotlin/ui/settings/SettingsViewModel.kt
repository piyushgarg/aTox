// SPDX-FileCopyrightText: 2019-2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.appearance.AppearanceManager
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.atox.tox.ToxStarter
import ltd.evilcorp.core.model.BootstrapNodeSource
import ltd.evilcorp.core.model.FtAutoAccept
import ltd.evilcorp.core.tox.bootstrap.BootstrapNodeJsonParser
import ltd.evilcorp.core.tox.bootstrap.BootstrapNodeRegistry
import ltd.evilcorp.core.tox.save.ProxyType
import ltd.evilcorp.core.tox.save.SaveOptions
import ltd.evilcorp.core.tox.save.ToxSaveStatus
import ltd.evilcorp.core.tox.save.testToxSave
import ltd.evilcorp.domain.tox.Tox
import ltd.evilcorp.domain.feature.FileTransferManager
import ltd.evilcorp.domain.backup.BackupDataProvider
import ltd.evilcorp.domain.backup.BackupUseCase

private const val TOX_SHUTDOWN_POLL_DELAY_MS = 200L

enum class ProxyStatus {
    Good,
    BadPort,
    BadHost,
    BadType,
    NotFound,
}

class SettingsViewModel @Inject constructor(
    private val context: Context,
    private val resolver: ContentResolver,
    private val settings: Settings,
    private val appearanceManager: AppearanceManager,
    private val toxStarter: ToxStarter,
    private val tox: Tox,
    private val nodeParser: BootstrapNodeJsonParser,
    private val nodeRegistry: BootstrapNodeRegistry,
    private val fileTransferManager: FileTransferManager,
    private val backupUseCase: BackupUseCase,
) : ViewModel() {
    private var restartNeeded = false

    private val _proxyStatus = MutableLiveData<ProxyStatus>()
    val proxyStatus: LiveData<ProxyStatus> get() = _proxyStatus

    private val _committed = MutableLiveData<Boolean>().apply { value = false }
    val committed: LiveData<Boolean> get() = _committed

    val backupProviders: List<BackupDataProvider> = backupUseCase.providers

    private val _backupExporting = MutableLiveData(false)
    val backupExporting: LiveData<Boolean> get() = _backupExporting

    private val _backupExportStatus = MutableLiveData<Boolean?>()
    val backupExportStatus: LiveData<Boolean?> get() = _backupExportStatus

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
        })
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
            val saveStatus = testToxSave(
                SaveOptions(saveData = null, getUdpEnabled(), getProxyType(), getProxyAddress(), getProxyPort()),
                null,
            )

            val proxyStatus = when (saveStatus) {
                ToxSaveStatus.BadProxyHost -> ProxyStatus.BadHost
                ToxSaveStatus.BadProxyPort -> ProxyStatus.BadPort
                ToxSaveStatus.BadProxyType -> ProxyStatus.BadType
                ToxSaveStatus.ProxyNotFound -> ProxyStatus.NotFound
                else -> ProxyStatus.Good
            }

            _proxyStatus.postValue(proxyStatus)
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

    fun isCurrentPassword(maybeCurrentPassword: String) = tox.password == maybeCurrentPassword.ifEmpty { null }

    fun setPassword(newPassword: String) = tox.changePassword(newPassword.ifEmpty { null })

    fun getBootstrapNodeSource(): BootstrapNodeSource = settings.bootstrapNodeSource
    fun setBootstrapNodeSource(source: BootstrapNodeSource) {
        settings.bootstrapNodeSource = source
        nodeRegistry.reset()
        restartNeeded = true
    }

    suspend fun validateNodeJson(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val bytes = resolver.openInputStream(uri)?.use {
            it.readBytes()
        } ?: return@withContext false

        return@withContext nodeParser.parse(bytes.decodeToString()).isNotEmpty()
    }

    suspend fun importNodeJson(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val bytes = resolver.openInputStream(uri)?.use {
            it.readBytes()
        } ?: return@withContext false

        val out = File(context.filesDir, "user_nodes.json")
        out.delete()
        if (!out.createNewFile()) return@withContext false

        out.outputStream().use { it.write(bytes) }
        return@withContext true
    }

    fun getDisableScreenshots(): Boolean = settings.disableScreenshots
    fun setDisableScreenshots(disable: Boolean) {
        settings.disableScreenshots = disable
    }

    fun getCacheSize(): Long = fileTransferManager.getCacheSize()
    fun clearCache() = fileTransferManager.clearCache()

    fun exportBackup(uri: Uri, selectedIds: Set<String>, password: String?) = viewModelScope.launch(Dispatchers.IO) {
        _backupExporting.postValue(true)
        val success = runCatching {
            val data = backupUseCase.export(selectedIds, password)
            resolver.openOutputStream(uri)?.use { it.write(data) } ?: error("Unable to open destination")
        }.isSuccess
        _backupExporting.postValue(false)
        _backupExportStatus.postValue(success)
    }

    fun consumeBackupExportStatus() {
        _backupExportStatus.value = null
    }
}
