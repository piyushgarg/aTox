package ltd.evilcorp.core.tox.runtime.delegates

import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.core.tox.NativeToxAv
import ltd.evilcorp.domain.tox.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.tox.enums.ToxGroupRole
import ltd.evilcorp.domain.tox.enums.ToxMessageType

class ToxGroupBridge(
    private val nativeTox: NativeTox,
    private val nativeToxAv: NativeToxAv,
    private val toxPtrProvider: () -> Long,
) {
    fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int = synchronized(this) {
        nativeTox.toxGroupNew(toxPtrProvider(), privacyState.value, groupName, selfName)
    }

    fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int = synchronized(this) {
        nativeTox.toxGroupJoin(toxPtrProvider(), friendNo, inviteData, selfName, password)
    }

    fun groupLeave(groupNumber: Int): Boolean = synchronized(this) {
        nativeTox.toxGroupLeave(toxPtrProvider(), groupNumber)
    }

    fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int = synchronized(this) {
        nativeTox.toxGroupSendMessage(toxPtrProvider(), groupNumber, type.ordinal, message)
    }

    fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean = synchronized(this) {
        nativeTox.toxGroupSetTopic(toxPtrProvider(), groupNumber, topic)
    }

    fun groupGetTopic(groupNumber: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupGetTopic(toxPtrProvider(), groupNumber)
    }

    fun groupGetName(groupNumber: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupGetName(toxPtrProvider(), groupNumber)
    }

    fun groupGetChatId(groupNumber: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupGetChatId(toxPtrProvider(), groupNumber)
    }

    fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean = synchronized(this) {
        nativeTox.toxGroupSetPassword(toxPtrProvider(), groupNumber, password)
    }

    fun groupGetPassword(groupNumber: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupGetPassword(toxPtrProvider(), groupNumber)
    }

    fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupPeerGetName(toxPtrProvider(), groupNumber, peerId)
    }

    fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupPeerGetPublicKey(toxPtrProvider(), groupNumber, peerId)
    }

    fun groupSelfGetPeerId(groupNumber: Int): Int = synchronized(this) {
        nativeTox.toxGroupSelfGetPeerId(toxPtrProvider(), groupNumber)
    }

    fun groupSelfGetRole(groupNumber: Int): ToxGroupRole = synchronized(this) {
        ToxGroupRole.fromInt(nativeTox.toxGroupSelfGetRole(toxPtrProvider(), groupNumber))
    }

    fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean = synchronized(this) {
        nativeTox.toxGroupInviteSend(toxPtrProvider(), groupNumber, friendNumber)
    }

    fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int = synchronized(this) {
        nativeTox.toxGroupJoinDirect(toxPtrProvider(), chatId, selfName, password)
    }

    fun groupReconnect(groupNumber: Int): Boolean = synchronized(this) {
        nativeTox.toxGroupReconnect(toxPtrProvider(), groupNumber)
    }

    fun groupavAdd(): Int = synchronized(this) {
        nativeToxAv.toxavAddAvGroupchat(toxPtrProvider())
    }

    fun groupavJoin(groupNumber: Int): Int = synchronized(this) {
        nativeToxAv.toxavJoinAvGroupchat(toxPtrProvider(), groupNumber)
    }

    fun groupavSendAudio(groupNumber: Int, pcm: ShortArray, channels: Int, samplingRate: Int): Int = synchronized(this) {
        nativeToxAv.toxavGroupSendAudio(toxPtrProvider(), groupNumber, pcm, pcm.size, channels, samplingRate)
    }

    fun groupavEnableAudio(groupNumber: Int): Int = synchronized(this) {
        nativeToxAv.toxavGroupchatEnableAv(toxPtrProvider(), groupNumber)
    }

    fun groupavDisableAudio(groupNumber: Int): Int = synchronized(this) {
        nativeToxAv.toxavGroupchatDisableAv(toxPtrProvider(), groupNumber)
    }

    fun groupavIsEnabled(groupNumber: Int): Boolean = synchronized(this) {
        nativeToxAv.toxavGroupchatAvEnabled(toxPtrProvider(), groupNumber)
    }
}
