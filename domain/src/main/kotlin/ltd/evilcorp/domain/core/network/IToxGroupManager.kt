// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network

import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxGroupRole
import ltd.evilcorp.domain.core.network.enums.ToxMessageType

@Suppress("ComplexInterface")
interface IToxGroupManager {
    fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int
    fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int
    fun groupLeave(groupNumber: Int): Boolean
    fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int
    fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean
    fun groupGetTopic(groupNumber: Int): ByteArray?
    fun groupGetName(groupNumber: Int): ByteArray?
    fun groupGetChatId(groupNumber: Int): ByteArray?
    fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean
    fun groupGetPassword(groupNumber: Int): ByteArray?
    fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray?
    fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray?
    fun groupSelfGetPeerId(groupNumber: Int): Int
    fun groupSelfGetRole(groupNumber: Int): ToxGroupRole
    fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean
    fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int
    fun groupReconnect(groupNumber: Int): Boolean
}
