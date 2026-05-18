// SPDX-FileCopyrightText: 2021 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.service

import android.util.Log
import java.util.Timer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.domain.feature.UserManager
import ltd.evilcorp.domain.tox.Tox

private const val TAG = "AutoAway"
private const val MILLIS_PER_SECOND = 1000L

@Singleton
class AutoAway @Inject constructor(
    private val scope: CoroutineScope,
    private val settings: Settings,
    private val userManager: UserManager,
    private val tox: Tox,
) {
    private var awayTimer = Timer()
    private var isAutoAway = false

    fun onBackground() {
        if (!settings.autoAwayEnabled) return

        Log.i(TAG, "In background, scheduling away")
        awayTimer.schedule(settings.autoAwaySeconds * MILLIS_PER_SECOND) {
            scope.launch {
                if (tox.getStatus() != UserStatus.None) return@launch
                Log.i(TAG, "Setting away")
                userManager.setStatus(UserStatus.Away)
                isAutoAway = true
            }
        }
    }

    fun onForeground() {
        if (!settings.autoAwayEnabled) return
        Log.i(TAG, "In foreground, canceling away")
        awayTimer.cancel()
        awayTimer = Timer()
        if (isAutoAway) {
            Log.i(TAG, "Restoring status")
            userManager.setStatus(UserStatus.None)
            isAutoAway = false
        }
    }
}
