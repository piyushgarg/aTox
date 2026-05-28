// SPDX-FileCopyrightText: 2019-2025 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.infrastructure.tox

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

import javax.inject.Inject
import ltd.evilcorp.atox.infrastructure.service.ToxService
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.features.transfer.reset
import ltd.evilcorp.domain.features.auth.UserManager
import ltd.evilcorp.domain.core.network.save.ISaveManager
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.core.tox.save.testToxSave
import ltd.evilcorp.core.tox.ToxImpl
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus

import ltd.evilcorp.domain.core.network.IToxStarter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "ToxStarter"

open class ToxStarter @Inject constructor(
    private val scope: CoroutineScope,
    private val fileTransferManager: FileTransferManager,
    private val saveManager: ISaveManager,
    private val userManager: UserManager,
    private val startupSynchronizer: ToxStartupSynchronizer,
    private val listenerCallbacks: EventListenerCallbacks,
    private val tox: ToxImpl,
    private val eventListener: ToxEventListener,
    private val avEventListener: ToxAvEventListener,
    private val context: Context,
    private val settings: Settings,
    private val groupEventProcessor: GroupEventProcessor,
) : IToxStarter {
    init {
        Log.d(TAG, "Initialized with GroupEventProcessor: ${groupEventProcessor.hashCode()}")
    }

    override fun startTox(save: ByteArray?, password: String?): ToxSaveStatus {
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

    override fun tryLoadTox(password: String?): ToxSaveStatus {
        val save = tryLoadSave() ?: return ToxSaveStatus.SaveNotFound
        val status = startTox(save, password)
        if (status == ToxSaveStatus.Ok) {
            scope.launch {
                userManager.verifyExists(tox.publicKey)
            }
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
