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
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.core.tox.save.testToxSave
import ltd.evilcorp.core.tox.ToxImpl
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus

import ltd.evilcorp.domain.core.network.IToxStarter

private const val TAG = "ToxStarter"

open class ToxStarter @Inject constructor(
    private val sessionManager: ToxSessionManager,
    private val listenerCoordinator: ToxListenerCoordinator,
    private val tox: ToxImpl,
    private val context: Context,
    private val settings: Settings,
) : IToxStarter {

    override fun startTox(save: ByteArray?, password: String?): ToxSaveStatus {
        listenerCoordinator.setupListeners()
        val options =
            SaveOptions(save, settings.udpEnabled, settings.proxyType, settings.proxyAddress, settings.proxyPort)
        try {
            tox.isBootstrapNeeded = true
            tox.start(options, password, listenerCoordinator.eventListener, listenerCoordinator.avEventListener)
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "Unknown error")
            return testToxSave(options, password)
        }

        sessionManager.onToxStarted(tox.publicKey)
        startService()
        return ToxSaveStatus.Ok
    }

    override fun stopTox() {
        context.stopService(Intent(context, ToxService::class.java))
    }

    override fun tryLoadTox(password: String?): ToxSaveStatus {
        val save = tryLoadSave() ?: return ToxSaveStatus.SaveNotFound
        return startTox(save, password)
    }

    private fun startService() = context.run {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startService(Intent(this, ToxService::class.java))
        } else {
            startForegroundService(Intent(this, ToxService::class.java))
        }
    }

    private fun tryLoadSave(): ByteArray? {
        val activeProfileId = ltd.evilcorp.core.profile.ProfileManager.getActiveProfileId(context)
        if (activeProfileId != ltd.evilcorp.core.profile.ProfileManager.DEFAULT_PROFILE_ID) {
            return sessionManager.loadSave(PublicKey(activeProfileId))
        }
        val firstSave = sessionManager.listSaves().firstOrNull() ?: return null
        return sessionManager.loadSave(PublicKey(firstSave))
    }
}
