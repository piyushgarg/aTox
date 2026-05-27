package ltd.evilcorp.core.tox

import ltd.evilcorp.core.tox.listener.ToxEventListener

class NativeToxCore(private val nativeTox: NativeTox) {
    fun toxNew(savedata: ByteArray?): Long = nativeTox.toxNew(savedata)
    
    fun toxNewWithOptions(
        savedata: ByteArray?,
        ipv6Enabled: Boolean,
        udpEnabled: Boolean,
        localDiscoveryEnabled: Boolean,
        proxyType: Int,
        proxyHost: String?,
        proxyPort: Int
    ): Long = nativeTox.toxNewWithOptions(
        savedata, ipv6Enabled, udpEnabled, localDiscoveryEnabled, proxyType, proxyHost, proxyPort
    )

    fun toxKill(tox: Long) = nativeTox.toxKill(tox)
    
    fun toxBootstrap(tox: Long, address: String, port: Int, publicKey: ByteArray) =
        nativeTox.toxBootstrap(tox, address, port, publicKey)

    fun toxAddTcpRelay(tox: Long, address: String, port: Int, publicKey: ByteArray) =
        nativeTox.toxAddTcpRelay(tox, address, port, publicKey)
    
    fun toxIterate(tox: Long, listener: ToxEventListener) =
        nativeTox.toxIterate(tox, listener)

    fun toxIterationInterval(tox: Long): Int = nativeTox.toxIterationInterval(tox)

    fun toxGetName(tox: Long): ByteArray = nativeTox.toxGetName(tox)

    fun toxSetName(tox: Long, name: ByteArray) = nativeTox.toxSetName(tox, name)

    fun toxGetStatusMessage(tox: Long): ByteArray = nativeTox.toxGetStatusMessage(tox)

    fun toxSetStatusMessage(tox: Long, msg: ByteArray) = nativeTox.toxSetStatusMessage(tox, msg)
    
    fun toxGetAddress(tox: Long): ByteArray = nativeTox.toxGetAddress(tox)

    fun toxGetPublicKey(tox: Long): ByteArray = nativeTox.toxGetPublicKey(tox)

    fun toxSelfGetSecretKey(tox: Long): ByteArray = nativeTox.toxSelfGetSecretKey(tox)

    fun toxSelfGetUdpPort(tox: Long): Int = nativeTox.toxSelfGetUdpPort(tox)

    fun toxSelfGetTcpPort(tox: Long): Int = nativeTox.toxSelfGetTcpPort(tox)

    fun toxSelfGetDhtId(tox: Long): ByteArray = nativeTox.toxSelfGetDhtId(tox)
    
    fun toxGetNospam(tox: Long): Int = nativeTox.toxGetNospam(tox)

    fun toxSetNospam(tox: Long, nospam: Int) = nativeTox.toxSetNospam(tox, nospam)
    
    fun toxGetSavedata(tox: Long): ByteArray = nativeTox.toxGetSavedata(tox)

    fun toxAddFriend(tox: Long, pubKey: ByteArray, message: ByteArray): Int =
        nativeTox.toxAddFriend(tox, pubKey, message)

    fun toxAddFriendNorequest(tox: Long, pubKey: ByteArray): Int =
        nativeTox.toxAddFriendNorequest(tox, pubKey)

    fun toxDeleteFriend(tox: Long, friendNumber: Int) = nativeTox.toxDeleteFriend(tox, friendNumber)
    
    fun toxGetFriendList(tox: Long): IntArray = nativeTox.toxGetFriendList(tox)

    fun toxGetFriendPublicKey(tox: Long, friendNumber: Int): ByteArray =
        nativeTox.toxGetFriendPublicKey(tox, friendNumber)

    fun toxFriendByPublicKey(tox: Long, pubKey: ByteArray): Int =
        nativeTox.toxFriendByPublicKey(tox, pubKey)

    fun toxFriendExists(tox: Long, friendNumber: Int): Boolean =
        nativeTox.toxFriendExists(tox, friendNumber)

    fun toxFriendGetName(tox: Long, friendNumber: Int): ByteArray =
        nativeTox.toxFriendGetName(tox, friendNumber)

    fun toxFriendGetStatusMessage(tox: Long, friendNumber: Int): ByteArray =
        nativeTox.toxFriendGetStatusMessage(tox, friendNumber)

    fun toxFriendGetStatus(tox: Long, friendNumber: Int): Int =
        nativeTox.toxFriendGetStatus(tox, friendNumber)

    fun toxFriendGetConnectionStatus(tox: Long, friendNumber: Int): Int =
        nativeTox.toxFriendGetConnectionStatus(tox, friendNumber)

    fun toxFriendGetTyping(tox: Long, friendNumber: Int): Boolean =
        nativeTox.toxFriendGetTyping(tox, friendNumber)

    fun toxFriendGetLastOnline(tox: Long, friendNumber: Int): Long =
        nativeTox.toxFriendGetLastOnline(tox, friendNumber)
    
    fun toxFriendSendMessage(tox: Long, friendNumber: Int, type: Int, message: ByteArray): Int =
        nativeTox.toxFriendSendMessage(tox, friendNumber, type, message)

    fun toxSetTyping(tox: Long, friendNumber: Int, typing: Boolean) =
        nativeTox.toxSetTyping(tox, friendNumber, typing)
    
    fun toxGetSelfUserStatus(tox: Long): Int = nativeTox.toxGetSelfUserStatus(tox)

    fun toxSetSelfUserStatus(tox: Long, status: Int) = nativeTox.toxSetSelfUserStatus(tox, status)
    
    fun toxFriendSendLosslessPacket(tox: Long, friendNumber: Int, data: ByteArray) =
        nativeTox.toxFriendSendLosslessPacket(tox, friendNumber, data)

    fun toxFriendSendLossyPacket(tox: Long, friendNumber: Int, data: ByteArray) =
        nativeTox.toxFriendSendLossyPacket(tox, friendNumber, data)

    fun getSalt(data: ByteArray): ByteArray? = nativeTox.getSalt(data)

    fun passKeyDeriveWithSalt(passphrase: ByteArray, salt: ByteArray): ByteArray? =
        nativeTox.passKeyDeriveWithSalt(passphrase, salt)

    fun passDecrypt(data: ByteArray, passkey: ByteArray): ByteArray? =
        nativeTox.passDecrypt(data, passkey)

    fun passEncrypt(data: ByteArray, passkey: ByteArray): ByteArray? =
        nativeTox.passEncrypt(data, passkey)
}
