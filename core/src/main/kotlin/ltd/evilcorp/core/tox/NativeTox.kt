package ltd.evilcorp.core.tox

import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.enums.ToxFileControl
import ltd.evilcorp.core.tox.enums.ToxFileKind
import ltd.evilcorp.core.tox.enums.ToxMessageType

class NativeTox {
    init {
        System.loadLibrary("nativetox")
    }

    external fun toxNew(savedata: ByteArray?): Long
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
    
    external fun toxGetNospam(tox: Long): Int
    external fun toxSetNospam(tox: Long, nospam: Int)
    
    external fun toxGetSavedata(tox: Long): ByteArray

    external fun toxAddFriend(tox: Long, pubKey: ByteArray, message: ByteArray): Int
    external fun toxAddFriendNorequest(tox: Long, pubKey: ByteArray): Int
    external fun toxDeleteFriend(tox: Long, friendNumber: Int)
    
    external fun toxGetFriendList(tox: Long): IntArray
    external fun toxGetFriendPublicKey(tox: Long, friendNumber: Int): ByteArray
    external fun toxFriendByPublicKey(tox: Long, pubKey: ByteArray): Int
    
    external fun toxFriendSendMessage(tox: Long, friendNumber: Int, type: Int, message: ByteArray): Int
    external fun toxSetTyping(tox: Long, friendNumber: Int, typing: Boolean)
    
    external fun toxGetSelfUserStatus(tox: Long): Int
    external fun toxSetSelfUserStatus(tox: Long, status: Int)
 
    external fun toxFileControl(tox: Long, friendNumber: Int, fileNumber: Int, control: Int)
    external fun toxFileSend(tox: Long, friendNumber: Int, kind: Int, fileSize: Long, fileId: ByteArray, filename: ByteArray): Int
    external fun toxFileSendChunk(tox: Long, friendNumber: Int, fileNumber: Int, position: Long, data: ByteArray)

    external fun toxFriendSendLosslessPacket(tox: Long, friendNumber: Int, data: ByteArray)

    // Crypto
    external fun getSalt(data: ByteArray): ByteArray?
    external fun passKeyDeriveWithSalt(passphrase: ByteArray, salt: ByteArray): ByteArray?
    external fun passDecrypt(data: ByteArray, passkey: ByteArray): ByteArray?
    external fun passEncrypt(data: ByteArray, passkey: ByteArray): ByteArray?
}
