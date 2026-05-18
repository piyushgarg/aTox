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

    /**
     * Создает новую текстовую конференцию (групповой чат).
     * @param tox Указатель на нативный инстанс Tox.
     * @return Уникальный номер созданной конференции (ID группы), либо -1 в случае ошибки.
     */
    external fun toxConferenceNew(tox: Long): Int

    /**
     * Удаляет существующую конференцию (выход из группы или ее закрытие).
     * @param tox Указатель на нативный инстанс Tox.
     * @param conferenceNumber Номер удаляемой конференции.
     */
    external fun toxConferenceDelete(tox: Long, conferenceNumber: Int)

    /**
     * Отправляет приглашение другу для входа в существующую конференцию.
     * @param tox Указатель на нативный инстанс Tox.
     * @param friendNumber Номер друга, которого мы приглашаем.
     * @param conferenceNumber Номер конференции, в которую приглашаем.
     */
    external fun toxConferenceInvite(tox: Long, friendNumber: Int, conferenceNumber: Int)

    /**
     * Принимает входящее приглашение и присоединяется к конференции.
     * @param tox Указатель на нативный инстанс Tox.
     * @param friendNumber Номер друга, приславшего приглашение.
     * @param cookie Бинарные данные (cookie) приглашения, полученные в коллбеке.
     * @return Номер присоединенной конференции (ID группы), либо -1 в случае ошибки.
     */
    external fun toxConferenceJoin(tox: Long, friendNumber: Int, cookie: ByteArray): Int

    /**
     * Отправляет сообщение в конференцию (групповой чат).
     * @param tox Указатель на нативный инстанс Tox.
     * @param conferenceNumber Номер конференции.
     * @param type Тип сообщения (например, обычный текст).
     * @param message Текст сообщения в виде байтового массива (UTF-8).
     * @return 1 в случае успешной отправки, 0 при ошибке.
     */
    external fun toxConferenceSendMessage(tox: Long, conferenceNumber: Int, type: Int, message: ByteArray): Int

    /**
     * Возвращает количество участников в конкретной конференции.
     * @param tox Указатель на нативный инстанс Tox.
     * @param conferenceNumber Номер конференции.
     * @return Количество участников, либо -1 при ошибке.
     */
    external fun toxConferenceGetPeerCount(tox: Long, conferenceNumber: Int): Int

    /**
     * Возвращает имя участника конференции по его порядковому номеру.
     * @param tox Указатель на нативный инстанс Tox.
     * @param conferenceNumber Номер конференции.
     * @param peerNumber Порядковый номер участника в группе.
     * @return Имя участника в формате байтового массива (UTF-8).
     */
    external fun toxConferenceGetPeerName(tox: Long, conferenceNumber: Int, peerNumber: Int): ByteArray

    /**
     * Возвращает публичный ключ (Tox PublicKey) участника конференции.
     * @param tox Указатель на нативный инстанс Tox.
     * @param conferenceNumber Номер конференции.
     * @param peerNumber Порядковый номер участника в группе.
     * @return Публичный ключ участника (32 байта).
     */
    external fun toxConferenceGetPeerPublicKey(tox: Long, conferenceNumber: Int, peerNumber: Int): ByteArray

    /**
     * Возвращает список номеров всех активных конференций, в которых состоит пользователь.
     * @param tox Указатель на нативный инстанс Tox.
     * @return Массив идентификаторов конференций.
     */
    external fun toxConferenceGetChatlist(tox: Long): IntArray

    /**
     * Возвращает тип конференции (текстовая или аудио/видео).
     * @param tox Указатель на нативный инстанс Tox.
     * @param conferenceNumber Номер конференции.
     * @return 0 для текстовой группы, 1 для A/V конференции, либо -1 в случае ошибки.
     */
    external fun toxConferenceGetType(tox: Long, conferenceNumber: Int): Int

    // Crypto
    external fun getSalt(data: ByteArray): ByteArray?
    external fun passKeyDeriveWithSalt(passphrase: ByteArray, salt: ByteArray): ByteArray?
    external fun passDecrypt(data: ByteArray, passkey: ByteArray): ByteArray?
    external fun passEncrypt(data: ByteArray, passkey: ByteArray): ByteArray?
}
