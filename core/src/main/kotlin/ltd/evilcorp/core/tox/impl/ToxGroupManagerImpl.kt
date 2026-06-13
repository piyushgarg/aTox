// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox.impl

import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.core.tox.runtime.ToxRuntime
import ltd.evilcorp.domain.core.network.IToxGroupManager
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxGroupRole
import ltd.evilcorp.domain.core.network.enums.ToxMessageType

@Singleton
class ToxGroupManagerImpl @Inject constructor(
    private val runtime: ToxRuntime,
) : IToxGroupManager {
    override fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int =
        runtime.groupNew(privacyState, groupName, selfName)

    override fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int =
        runtime.groupJoin(friendNo, inviteData, selfName, password)

    override fun groupLeave(groupNumber: Int): Boolean =
        runtime.groupLeave(groupNumber)

    override fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int =
        runtime.groupSendMessage(groupNumber, type, message)

    override fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean =
        runtime.groupSetTopic(groupNumber, topic)

    override fun groupGetTopic(groupNumber: Int): ByteArray? =
        runtime.groupGetTopic(groupNumber)

    override fun groupGetName(groupNumber: Int): ByteArray? =
        runtime.groupGetName(groupNumber)

    override fun groupGetChatId(groupNumber: Int): ByteArray? =
        runtime.groupGetChatId(groupNumber)

    override fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean =
        runtime.groupSetPassword(groupNumber, password)

    override fun groupGetPassword(groupNumber: Int): ByteArray? =
        runtime.groupGetPassword(groupNumber)

    override fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? =
        runtime.groupPeerGetName(groupNumber, peerId)

    override fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? =
        runtime.groupPeerGetPublicKey(groupNumber, peerId)

    override fun groupSelfGetPeerId(groupNumber: Int): Int =
        runtime.groupSelfGetPeerId(groupNumber)

    override fun groupSelfGetRole(groupNumber: Int): ToxGroupRole =
        runtime.groupSelfGetRole(groupNumber)

    override fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean =
        runtime.groupInviteSend(groupNumber, friendNumber)

    override fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int =
        runtime.groupJoinDirect(chatId, selfName, password)

    override fun groupReconnect(groupNumber: Int): Boolean =
        runtime.groupReconnect(groupNumber)

    override fun groupGetChatlist(): IntArray =
        runtime.groupGetChatlist()
}
