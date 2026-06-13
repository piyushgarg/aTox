// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

class AtoxHapticFeedback(
    private val delegate: HapticFeedback,
    private val enabledProvider: () -> Boolean
) : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        if (enabledProvider()) {
            delegate.performHapticFeedback(hapticFeedbackType)
        }
    }
}
