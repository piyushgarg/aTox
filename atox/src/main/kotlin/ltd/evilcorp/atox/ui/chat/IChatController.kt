// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat

import android.net.Uri
import ltd.evilcorp.domain.features.chat.model.MessageType

interface IChatController {
    fun sendMessage(message: String, type: MessageType = MessageType.Normal)
    fun sendFile(uri: Uri)
    fun sendVoice(uri: Uri)
    fun acceptFileTransfer(id: Int)
    fun rejectFileTransfer(id: Int)
    fun setDraftMessage(draft: String)
}
