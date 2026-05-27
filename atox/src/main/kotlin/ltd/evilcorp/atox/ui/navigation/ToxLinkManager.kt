// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.navigation

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ToxLinkManager @Inject constructor() {
    private val _pendingToxId = MutableStateFlow<String?>(null)
    val pendingToxId: StateFlow<String?> = _pendingToxId.asStateFlow()

    fun setPendingToxId(toxId: String?) {
        _pendingToxId.value = toxId
    }

    fun clear() {
        _pendingToxId.value = null
    }
}
