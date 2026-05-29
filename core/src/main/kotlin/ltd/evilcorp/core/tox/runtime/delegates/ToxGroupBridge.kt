package ltd.evilcorp.core.tox.runtime.delegates

import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.core.tox.NativeToxAv
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxGroupRole
import ltd.evilcorp.domain.core.network.enums.ToxMessageType

class ToxGroupBridge(
    private val nativeTox: NativeTox,
    private val nativeToxAv: NativeToxAv,
    private val lock: Any,
    private val toxPtrProvider: () -> Long,
) {
    fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int = synchronized(lock) {
        nativeTox.toxGroupNew(toxPtrProvider(), privacyState.value, groupName, selfName)
    }

    fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int = synchronized(lock) {
        nativeTox.toxGroupJoin(toxPtrProvider(), friendNo, inviteData, selfName, password)
    }

    fun groupLeave(groupNumber: Int): Boolean = synchronized(lock) {
        nativeTox.toxGroupLeave(toxPtrProvider(), groupNumber)
    }

    fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int = synchronized(lock) {
        nativeTox.toxGroupSendMessage(toxPtrProvider(), groupNumber, type.ordinal, message)
    }

    fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean = synchronized(lock) {
        nativeTox.toxGroupSetTopic(toxPtrProvider(), groupNumber, topic)
    }

    fun groupGetTopic(groupNumber: Int): ByteArray? = synchronized(lock) {
        nativeTox.toxGroupGetTopic(toxPtrProvider(), groupNumber)
    }

    fun groupGetName(groupNumber: Int): ByteArray? = synchronized(lock) {
        nativeTox.toxGroupGetName(toxPtrProvider(), groupNumber)
    }

    fun groupGetChatId(groupNumber: Int): ByteArray? = synchronized(lock) {
        nativeTox.toxGroupGetChatId(toxPtrProvider(), groupNumber)
    }

    fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean = synchronized(lock) {
        nativeTox.toxGroupSetPassword(toxPtrProvider(), groupNumber, password)
    }

    fun groupGetPassword(groupNumber: Int): ByteArray? = synchronized(lock) {
        nativeTox.toxGroupGetPassword(toxPtrProvider(), groupNumber)
    }

    fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? = synchronized(lock) {
        nativeTox.toxGroupPeerGetName(toxPtrProvider(), groupNumber, peerId)
    }

    fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? = synchronized(lock) {
        nativeTox.toxGroupPeerGetPublicKey(toxPtrProvider(), groupNumber, peerId)
    }

    fun groupSelfGetPeerId(groupNumber: Int): Int = synchronized(lock) {
        nativeTox.toxGroupSelfGetPeerId(toxPtrProvider(), groupNumber)
    }

    fun groupSelfGetRole(groupNumber: Int): ToxGroupRole = synchronized(lock) {
        ToxGroupRole.fromInt(nativeTox.toxGroupSelfGetRole(toxPtrProvider(), groupNumber))
    }

    fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean = synchronized(lock) {
        nativeTox.toxGroupInviteSend(toxPtrProvider(), groupNumber, friendNumber)
    }

    fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int = synchronized(lock) {
        nativeTox.toxGroupJoinDirect(toxPtrProvider(), chatId, selfName, password)
    }

    fun groupReconnect(groupNumber: Int): Boolean = synchronized(lock) {
        nativeTox.toxGroupReconnect(toxPtrProvider(), groupNumber)
    }

    fun groupGetChatlist(): IntArray = synchronized(lock) {
        nativeTox.toxGroupGetChatlist(toxPtrProvider())
    }

    fun groupavAdd(): Int = synchronized(lock) {
        nativeToxAv.toxavAddAvGroupchat(toxPtrProvider())
    }

    fun groupavJoin(groupNumber: Int): Int = synchronized(lock) {
        nativeToxAv.toxavJoinAvGroupchat(toxPtrProvider(), groupNumber)
    }

    fun groupavSendAudio(groupNumber: Int, pcm: ShortArray, channels: Int, samplingRate: Int): Int = synchronized(lock) {
        nativeToxAv.toxavGroupSendAudio(toxPtrProvider(), groupNumber, pcm, pcm.size, channels, samplingRate)
    }

    fun groupavEnableAudio(groupNumber: Int): Int = synchronized(lock) {
        nativeToxAv.toxavGroupchatEnableAv(toxPtrProvider(), groupNumber)
    }

    fun groupavDisableAudio(groupNumber: Int): Int = synchronized(lock) {
        nativeToxAv.toxavGroupchatDisableAv(toxPtrProvider(), groupNumber)
    }

    fun groupavIsEnabled(groupNumber: Int): Boolean = synchronized(lock) {
        nativeToxAv.toxavGroupchatAvEnabled(toxPtrProvider(), groupNumber)
    }
}
