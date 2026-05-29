// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.contacts.model

import ltd.evilcorp.domain.core.network.enums.ToxMessageType

sealed class ToxFriendEvent {
    data class FriendStatusMessage(val publicKey: String, val message: String) : ToxFriendEvent()
    data class FriendReadReceipt(val publicKey: String, val messageId: Int) : ToxFriendEvent()
    data class FriendStatus(val publicKey: String, val status: UserStatus) : ToxFriendEvent()
    data class FriendConnectionStatus(val publicKey: String, val status: ConnectionStatus) : ToxFriendEvent()
    data class FriendRequest(val publicKey: String, val message: String) : ToxFriendEvent()
    data class FriendMessage(val publicKey: String, val type: ToxMessageType, val message: String) : ToxFriendEvent()
    data class FriendName(val publicKey: String, val newName: String) : ToxFriendEvent()
    data class SelfConnectionStatus(val status: ConnectionStatus) : ToxFriendEvent()
    data class FriendTyping(val publicKey: String, val isTyping: Boolean) : ToxFriendEvent()
}
