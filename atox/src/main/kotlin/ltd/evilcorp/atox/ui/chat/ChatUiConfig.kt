// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat

import ltd.evilcorp.domain.model.DateFormatPreference
import ltd.evilcorp.domain.model.TimeFormatPreference

data class ChatUiConfig(
    val hapticEnabled: Boolean,
    val dateFormatPreference: DateFormatPreference,
    val timeFormatPreference: TimeFormatPreference,
    val sentMessageSoundUri: String,
    val sentMessageSoundVolume: Int,
    val enableReplies: Boolean,
)
