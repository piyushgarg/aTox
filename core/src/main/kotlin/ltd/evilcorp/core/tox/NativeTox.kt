package ltd.evilcorp.core.tox

import ltd.evilcorp.core.tox.listener.ToxEventListener

/**
 * Low-level JNI bridge to the native C/C++ Tox library (libnativetox).
 * Member signatures must strictly match compiled JNI headers.
 */
@Suppress("unused")
class NativeTox {
    companion object {
        init {
            System.loadLibrary("nativetox")
        }
    }

    external fun toxNew(savedata: ByteArray?): Long
    external fun toxNewWithOptions(
        savedata: ByteArray?,
        ipv6Enabled: Boolean,
        udpEnabled: Boolean,
        localDiscoveryEnabled: Boolean,
        proxyType: Int,
        proxyHost: String?,
        proxyPort: Int
    ): Long
    external fun toxKill(tox: Long)
    
    external fun toxBootstrap(tox: Long, address: String, port: Int, publicKey: ByteArray)
    external fun toxAddTcpRelay(tox: Long, address: String, port: Int, publicKey: ByteArray)
    
    external fun toxIterate(tox: Long, listener: ToxEventListener)
    external fun toxIterationInterval(tox: Long): Int

    external fun toxGetName(tox: Long): ByteArray
    external fun toxSetName(tox: Long, name: ByteArray)

    external fun toxGetStatusMessage(tox: Long): ByteArray
    external fun toxSetStatusMessage(tox: Long, msg: ByteArray)
    
    external fun toxGetAddress(tox: Long): ByteArray
    external fun toxGetPublicKey(tox: Long): ByteArray
    external fun toxSelfGetSecretKey(tox: Long): ByteArray

    external fun toxSelfGetUdpPort(tox: Long): Int
    external fun toxSelfGetTcpPort(tox: Long): Int
    external fun toxSelfGetDhtId(tox: Long): ByteArray
    
    external fun toxGetNospam(tox: Long): Int
    external fun toxSetNospam(tox: Long, nospam: Int)
    
    external fun toxGetSavedata(tox: Long): ByteArray

    // Contact management APIs
    external fun toxAddFriend(tox: Long, pubKey: ByteArray, message: ByteArray): Int
    external fun toxAddFriendNorequest(tox: Long, pubKey: ByteArray): Int
    external fun toxDeleteFriend(tox: Long, friendNumber: Int)
    external fun toxGetFriendList(tox: Long): IntArray
    external fun toxGetFriendPublicKey(tox: Long, friendNumber: Int): ByteArray
    external fun toxFriendByPublicKey(tox: Long, pubKey: ByteArray): Int
    external fun toxFriendExists(tox: Long, friendNumber: Int): Boolean
    external fun toxFriendGetName(tox: Long, friendNumber: Int): ByteArray
    external fun toxFriendGetStatusMessage(tox: Long, friendNumber: Int): ByteArray
    external fun toxFriendGetStatus(tox: Long, friendNumber: Int): Int
    external fun toxFriendGetConnectionStatus(tox: Long, friendNumber: Int): Int
    external fun toxFriendGetTyping(tox: Long, friendNumber: Int): Boolean
    external fun toxFriendGetLastOnline(tox: Long, friendNumber: Int): Long
    
    // Messaging & typing APIs
    external fun toxFriendSendMessage(tox: Long, friendNumber: Int, type: Int, message: ByteArray): Int
    external fun toxSetTyping(tox: Long, friendNumber: Int, typing: Boolean)
    external fun toxGetSelfUserStatus(tox: Long): Int
    external fun toxSetSelfUserStatus(tox: Long, status: Int)
 
    // File transfer APIs
    external fun toxFileControl(tox: Long, friendNumber: Int, fileNumber: Int, control: Int)
    external fun toxFileSend(tox: Long, friendNumber: Int, kind: Int, fileSize: Long, fileId: ByteArray, filename: ByteArray): Int
    external fun toxFileSendChunk(tox: Long, friendNumber: Int, fileNumber: Int, position: Long, data: ByteArray)
    external fun toxFileGetFileId(tox: Long, friendNumber: Int, fileNumber: Int): ByteArray

    // Custom P2P packet transmission
    external fun toxFriendSendLosslessPacket(tox: Long, friendNumber: Int, data: ByteArray)
    external fun toxFriendSendLossyPacket(tox: Long, friendNumber: Int, data: ByteArray)

    // Legacy group conference APIs
    external fun toxConferenceNew(tox: Long): Int
    external fun toxConferenceDelete(tox: Long, conferenceNumber: Int)
    external fun toxConferenceInvite(tox: Long, friendNumber: Int, conferenceNumber: Int)
    external fun toxConferenceJoin(tox: Long, friendNumber: Int, cookie: ByteArray): Int
    external fun toxConferenceSendMessage(tox: Long, conferenceNumber: Int, type: Int, message: ByteArray): Int
    external fun toxConferenceSetTitle(tox: Long, conferenceNumber: Int, title: ByteArray)
    external fun toxConferenceGetTitle(tox: Long, conferenceNumber: Int): ByteArray
    external fun toxConferencePeerNumberIsOurself(tox: Long, conferenceNumber: Int, peerNumber: Int): Boolean
    external fun toxConferenceGetPeerCount(tox: Long, conferenceNumber: Int): Int
    external fun toxConferenceGetPeerName(tox: Long, conferenceNumber: Int, peerNumber: Int): ByteArray
    external fun toxConferenceGetPeerPublicKey(tox: Long, conferenceNumber: Int, peerNumber: Int): ByteArray
    external fun toxConferenceGetChatlist(tox: Long): IntArray
    external fun toxConferenceGetType(tox: Long, conferenceNumber: Int): Int

    // Next Generation Conferences (NGC Groups)
    external fun toxGroupNew(tox: Long, privacyState: Int, groupName: ByteArray, selfName: ByteArray): Int
    external fun toxGroupJoin(tox: Long, friendNumber: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int
    external fun toxGroupLeave(tox: Long, groupNumber: Int): Boolean
    external fun toxGroupSendMessage(tox: Long, groupNumber: Int, type: Int, message: ByteArray): Int
    external fun toxGroupSetTopic(tox: Long, groupNumber: Int, topic: ByteArray): Boolean
    external fun toxGroupGetTopic(tox: Long, groupNumber: Int): ByteArray?
    external fun toxGroupGetName(tox: Long, groupNumber: Int): ByteArray?
    external fun toxGroupGetChatId(tox: Long, groupNumber: Int): ByteArray?
    external fun toxGroupSetPassword(tox: Long, groupNumber: Int, password: ByteArray?): Boolean
    external fun toxGroupGetPassword(tox: Long, groupNumber: Int): ByteArray?
    external fun toxGroupPeerGetName(tox: Long, groupNumber: Int, peerId: Int): ByteArray?
    external fun toxGroupPeerGetPublicKey(tox: Long, groupNumber: Int, peerId: Int): ByteArray?
    external fun toxGroupSelfGetPeerId(tox: Long, groupNumber: Int): Int
    external fun toxGroupSelfGetRole(tox: Long, groupNumber: Int): Int
    external fun toxGroupInviteSend(tox: Long, groupNumber: Int, friendNumber: Int): Boolean
    external fun toxGroupJoinDirect(tox: Long, chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int
    external fun toxGroupReconnect(tox: Long, groupNumber: Int): Boolean
    external fun toxGroupGetChatlist(tox: Long): IntArray

    // Encryption & decryption profiles
    external fun getSalt(data: ByteArray): ByteArray?
    external fun passKeyDeriveWithSalt(passphrase: ByteArray, salt: ByteArray): ByteArray?
    external fun passDecrypt(data: ByteArray, passkey: ByteArray): ByteArray?
    external fun passEncrypt(data: ByteArray, passkey: ByteArray): ByteArray?
}
