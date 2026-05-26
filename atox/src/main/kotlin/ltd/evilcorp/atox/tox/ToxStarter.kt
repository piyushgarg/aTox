// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.tox

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

import javax.inject.Inject
import ltd.evilcorp.atox.service.ToxService
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.domain.model.PublicKey
import ltd.evilcorp.domain.feature.FileTransferManager
import ltd.evilcorp.domain.feature.UserManager
import ltd.evilcorp.core.tox.save.SaveManager
import ltd.evilcorp.domain.tox.save.SaveOptions
import ltd.evilcorp.core.tox.save.testToxSave
import ltd.evilcorp.core.tox.Tox
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.save.ToxSaveStatus

import ltd.evilcorp.domain.tox.IToxStarter

private const val TAG = "ToxStarter"

class ToxStarter @Inject constructor(
    private val fileTransferManager: FileTransferManager,
    private val saveManager: SaveManager,
    private val userManager: UserManager,
    private val startupSynchronizer: ToxStartupSynchronizer,
    private val listenerCallbacks: EventListenerCallbacks,
    private val tox: Tox,
    private val eventListener: ToxEventListener,
    private val avEventListener: ToxAvEventListener,
    private val context: Context,
    private val settings: Settings,
) : IToxStarter {
    fun startTox(save: ByteArray? = null, password: String? = null): ToxSaveStatus {
        listenerCallbacks.setUp(eventListener)
        listenerCallbacks.setUp(avEventListener)
        val options =
            SaveOptions(save, settings.udpEnabled, settings.proxyType, settings.proxyAddress, settings.proxyPort)
        try {
            tox.isBootstrapNeeded = true
            tox.start(options, password, eventListener, avEventListener)
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "Unknown error")
            return testToxSave(options, password)
        }

        startupSynchronizer.synchronizeAfterStart()

        // This can stay alive across core restarts and it doesn't work well when toxcore resets its numbers
        fileTransferManager.reset()
        startService()
        return ToxSaveStatus.Ok
    }

    override fun stopTox() {
        context.stopService(Intent(context, ToxService::class.java))
    }

    fun tryLoadTox(password: String?): ToxSaveStatus {
        val save = tryLoadSave() ?: return ToxSaveStatus.SaveNotFound
        val status = startTox(save, password)
        if (status == ToxSaveStatus.Ok) {
            userManager.verifyExists(tox.publicKey)
        }
        return status
    }

    private fun startService() = context.run {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startService(Intent(this, ToxService::class.java))
        } else {
            startForegroundService(Intent(this, ToxService::class.java))
        }
    }

    private fun tryLoadSave(): ByteArray? = saveManager.run { list().firstOrNull()?.let { load(PublicKey(it)) } }
}
