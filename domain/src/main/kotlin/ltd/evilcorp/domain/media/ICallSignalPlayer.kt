// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.media

import kotlinx.coroutines.CoroutineScope

interface ICallSignalPlayer {
    fun playIncomingRingtone(scope: CoroutineScope)
    fun playRingback(scope: CoroutineScope, isCallActive: () -> Boolean)
    fun stopSignals()
}
