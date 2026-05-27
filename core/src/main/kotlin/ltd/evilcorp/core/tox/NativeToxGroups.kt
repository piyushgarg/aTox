package ltd.evilcorp.core.tox

class NativeToxGroups(private val nativeTox: NativeTox) {
    fun toxConferenceNew(tox: Long): Int = nativeTox.toxConferenceNew(tox)

    fun toxConferenceDelete(tox: Long, conferenceNumber: Int) =
        nativeTox.toxConferenceDelete(tox, conferenceNumber)

    fun toxConferenceInvite(tox: Long, friendNumber: Int, conferenceNumber: Int) =
        nativeTox.toxConferenceInvite(tox, friendNumber, conferenceNumber)

    fun toxConferenceJoin(tox: Long, friendNumber: Int, cookie: ByteArray): Int =
        nativeTox.toxConferenceJoin(tox, friendNumber, cookie)

    fun toxConferenceSendMessage(tox: Long, conferenceNumber: Int, type: Int, message: ByteArray): Int =
        nativeTox.toxConferenceSendMessage(tox, conferenceNumber, type, message)

    fun toxConferenceSetTitle(tox: Long, conferenceNumber: Int, title: ByteArray) =
        nativeTox.toxConferenceSetTitle(tox, conferenceNumber, title)

    fun toxConferenceGetTitle(tox: Long, conferenceNumber: Int): ByteArray =
        nativeTox.toxConferenceGetTitle(tox, conferenceNumber)

    fun toxConferencePeerNumberIsOurself(tox: Long, conferenceNumber: Int, peerNumber: Int): Boolean =
        nativeTox.toxConferencePeerNumberIsOurself(tox, conferenceNumber, peerNumber)

    fun toxConferenceGetPeerCount(tox: Long, conferenceNumber: Int): Int =
        nativeTox.toxConferenceGetPeerCount(tox, conferenceNumber)

    fun toxConferenceGetPeerName(tox: Long, conferenceNumber: Int, peerNumber: Int): ByteArray =
        nativeTox.toxConferenceGetPeerName(tox, conferenceNumber, peerNumber)

    fun toxConferenceGetPeerPublicKey(tox: Long, conferenceNumber: Int, peerNumber: Int): ByteArray =
        nativeTox.toxConferenceGetPeerPublicKey(tox, conferenceNumber, peerNumber)

    fun toxConferenceGetChatlist(tox: Long): IntArray = nativeTox.toxConferenceGetChatlist(tox)

    fun toxConferenceGetType(tox: Long, conferenceNumber: Int): Int =
        nativeTox.toxConferenceGetType(tox, conferenceNumber)

    fun toxGroupNew(tox: Long, privacyState: Int, groupName: ByteArray, selfName: ByteArray): Int =
        nativeTox.toxGroupNew(tox, privacyState, groupName, selfName)

    fun toxGroupJoin(tox: Long, friendNumber: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int =
        nativeTox.toxGroupJoin(tox, friendNumber, inviteData, selfName, password)

    fun toxGroupLeave(tox: Long, groupNumber: Int): Boolean =
        nativeTox.toxGroupLeave(tox, groupNumber)

    fun toxGroupSendMessage(tox: Long, groupNumber: Int, type: Int, message: ByteArray): Int =
        nativeTox.toxGroupSendMessage(tox, groupNumber, type, message)

    fun toxGroupSetTopic(tox: Long, groupNumber: Int, topic: ByteArray): Boolean =
        nativeTox.toxGroupSetTopic(tox, groupNumber, topic)

    fun toxGroupGetTopic(tox: Long, groupNumber: Int): ByteArray? =
        nativeTox.toxGroupGetTopic(tox, groupNumber)

    fun toxGroupGetName(tox: Long, groupNumber: Int): ByteArray? =
        nativeTox.toxGroupGetName(tox, groupNumber)

    fun toxGroupGetChatId(tox: Long, groupNumber: Int): ByteArray? =
        nativeTox.toxGroupGetChatId(tox, groupNumber)

    fun toxGroupSetPassword(tox: Long, groupNumber: Int, password: ByteArray?): Boolean =
        nativeTox.toxGroupSetPassword(tox, groupNumber, password)

    fun toxGroupGetPassword(tox: Long, groupNumber: Int): ByteArray? =
        nativeTox.toxGroupGetPassword(tox, groupNumber)

    fun toxGroupPeerGetName(tox: Long, groupNumber: Int, peerId: Int): ByteArray? =
        nativeTox.toxGroupPeerGetName(tox, groupNumber, peerId)

    fun toxGroupPeerGetPublicKey(tox: Long, groupNumber: Int, peerId: Int): ByteArray? =
        nativeTox.toxGroupPeerGetPublicKey(tox, groupNumber, peerId)

    fun toxGroupSelfGetPeerId(tox: Long, groupNumber: Int): Int =
        nativeTox.toxGroupSelfGetPeerId(tox, groupNumber)

    fun toxGroupSelfGetRole(tox: Long, groupNumber: Int): Int =
        nativeTox.toxGroupSelfGetRole(tox, groupNumber)

    fun toxGroupInviteSend(tox: Long, groupNumber: Int, friendNumber: Int): Boolean =
        nativeTox.toxGroupInviteSend(tox, groupNumber, friendNumber)

    fun toxGroupJoinDirect(tox: Long, chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int =
        nativeTox.toxGroupJoinDirect(tox, chatId, selfName, password)

    fun toxGroupReconnect(tox: Long, groupNumber: Int): Boolean =
        nativeTox.toxGroupReconnect(tox, groupNumber)
}
