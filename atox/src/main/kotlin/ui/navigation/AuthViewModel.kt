// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.navigation

import androidx.lifecycle.ViewModel
import javax.inject.Inject
import ltd.evilcorp.atox.tox.ToxStarter
import ltd.evilcorp.core.tox.save.ToxSaveStatus
import ltd.evilcorp.domain.tox.Tox

class AuthViewModel @Inject constructor(
    private val tox: Tox,
    private val toxStarter: ToxStarter,
) : ViewModel() {
    val publicKey by lazy { tox.publicKey }

    fun isToxRunning(): Boolean = tox.started

    fun tryLoadTox(password: String?): ToxSaveStatus {
        return toxStarter.tryLoadTox(password)
    }

    fun quitTox() {
        toxStarter.stopTox()
    }
}
