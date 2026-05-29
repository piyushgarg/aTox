// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.infrastructure.tox

import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.domain.features.contacts.IToxFriendEventBus
import ltd.evilcorp.domain.features.contacts.model.ToxFriendEvent
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.core.network.enums.ToxMessageType

@Singleton
class FriendEventHandler @Inject constructor(
    private val eventBus: IToxFriendEventBus,
) {
    fun onFriendStatusMessage(publicKey: String, message: String) {
        eventBus.tryEmit(ToxFriendEvent.FriendStatusMessage(publicKey, message))
    }

    fun onFriendReadReceipt(publicKey: String, messageId: Int) {
        eventBus.tryEmit(ToxFriendEvent.FriendReadReceipt(publicKey, messageId))
    }

    fun onFriendStatus(publicKey: String, status: UserStatus) {
        eventBus.tryEmit(ToxFriendEvent.FriendStatus(publicKey, status))
    }

    fun onFriendConnectionStatus(publicKey: String, status: ConnectionStatus) {
        eventBus.tryEmit(ToxFriendEvent.FriendConnectionStatus(publicKey, status))
    }

    fun onFriendRequest(publicKey: String, message: String) {
        eventBus.tryEmit(ToxFriendEvent.FriendRequest(publicKey, message))
    }

    fun onFriendMessage(publicKey: String, type: ToxMessageType, message: String) {
        eventBus.tryEmit(ToxFriendEvent.FriendMessage(publicKey, type, message))
    }

    fun onFriendName(publicKey: String, newName: String) {
        eventBus.tryEmit(ToxFriendEvent.FriendName(publicKey, newName))
    }

    fun onSelfConnectionStatus(status: ConnectionStatus) {
        eventBus.tryEmit(ToxFriendEvent.SelfConnectionStatus(status))
    }

    fun onFriendTyping(publicKey: String, isTyping: Boolean) {
        eventBus.tryEmit(ToxFriendEvent.FriendTyping(publicKey, isTyping))
    }
}
