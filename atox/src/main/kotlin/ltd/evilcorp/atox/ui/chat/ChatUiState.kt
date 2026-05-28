// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat

import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.chat.model.Message

data class ChatUiState(
    val contact: Contact? = null,
    val messages: List<Message> = emptyList(),
    val fileTransfers: List<FileTransfer> = emptyList(),
    val isTyping: Boolean = false,
    val callState: CallAvailability = CallAvailability.Unavailable,
    val uiConfig: ChatUiConfig? = null,
    val replyingToMessage: Message? = null
)
