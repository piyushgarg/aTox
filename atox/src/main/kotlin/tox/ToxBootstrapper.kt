// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.tox

import android.util.Log
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.domain.tox.Tox
import java.util.Timer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.schedule

private const val TAG = "ToxBootstrapper"
private const val BOOTSTRAP_INTERVAL_MS = 60_000L

@Singleton
class ToxBootstrapper @Inject constructor(
    private val tox: Tox
) {
    private var bootstrapTimer = Timer()

    @Synchronized
    fun updateConnectionStatus(connectionStatus: ConnectionStatus) {
        if (connectionStatus == ConnectionStatus.None) {
            Log.i(TAG, "Gone offline, scheduling bootstrap")
            bootstrapTimer.cancel()
            bootstrapTimer = Timer()
            bootstrapTimer.schedule(BOOTSTRAP_INTERVAL_MS, BOOTSTRAP_INTERVAL_MS) {
                Log.i(TAG, "Been offline for too long, bootstrapping")
                tox.isBootstrapNeeded = true
            }
        } else {
            Log.i(TAG, "Online, cancelling bootstrap")
            bootstrapTimer.cancel()
            bootstrapTimer = Timer()
        }
    }

    @Synchronized
    fun cancel() {
        bootstrapTimer.cancel()
        bootstrapTimer = Timer()
    }
}
