package ltd.evilcorp.core.tox.runtime

import android.util.Log
import kotlin.random.Random
import ltd.evilcorp.core.model.FileKind
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.core.tox.NativeToxAv
import ltd.evilcorp.core.tox.ToxID
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.enums.ToxavCallControl
import ltd.evilcorp.core.tox.enums.ToxFileControl
import ltd.evilcorp.core.tox.enums.ToxMessageType
import ltd.evilcorp.core.tox.enums.ToxGroupPrivacyState
import ltd.evilcorp.core.tox.enums.ToxGroupRole
import ltd.evilcorp.core.tox.save.SaveOptions
import ltd.evilcorp.core.tox.toToxtype
import ltd.evilcorp.core.tox.toToxType

private const val TAG = "ToxWrapper"

private const val FILE_ID_LENGTH = 32

/**
 * Перечисление возможных ошибок при отправке пользовательских P2P-пакетов.
 */
enum class CustomPacketError {
    /** Успешная отправка пакета. */
    Success,
    /** Пакет пуст. */
    Empty,
    /** Друг в данный момент не в сети. */
    FriendNotConnected,
    /** Указанный друг не найден в контакт-листе. */
    FriendNotFound,
    /** Неверные параметры пакета. */
    Invalid,
    /** Ссылка на буфер пакета равна null. */
    Null,
    /** Очередь отправки пакетов переполнена. */
    Sendq,
    /** Превышена максимальная длина пакета. */
    TooLong,
}

/**
 * Высокоуровневая потокобезопасная обертка над нативными библиотеками NativeTox и NativeToxAv.
 * Управляет жизненным циклом сессии Tox, вызовами сетевых функций, файловыми трансферами и AV-звонками.
 */
class ToxWrapper(
    private val eventListener: ToxEventListener,
    private val avEventListener: ToxAvEventListener,
    options: SaveOptions,
) {
    private val nativeTox = NativeTox()
    private val nativeToxAv = NativeToxAv()
    private var toxPtr: Long = 0
    private var toxavPtr: Long = 0

    /**
     * Динамически настраиваемый битрейт аудио Opus (в kbps).
     * Рекомендуемое значение: 32 kbps (достаточно для качественного полнополосного стерео-звука).
     */
    @Volatile
    var audioBitrate: Int = 32

    init {
        val sd = options.saveData
        toxPtr = nativeTox.toxNew(sd)
        if (toxPtr != 0L) {
            toxavPtr = nativeToxAv.toxavNew(toxPtr)
        }
        updateContactMapping()
    }

    /**
     * Обновляет внутреннюю карту соответствия номеров друзей и их публичных ключей
     * для слушателей событий ядра и AV-звонков.
     */
    private fun updateContactMapping() {
        val contacts = getContacts()
        eventListener.contactMapping = contacts
        avEventListener.contactMapping = contacts
    }

    /**
     * Выполняет первичное подключение (bootstrap) к глобальной сети DHT Tox
     * через публичный или приватный узел, а также регистрирует TCP-релей.
     *
     * @param address IP-адрес или доменное имя узла.
     * @param port Сетевой порт узла.
     * @param publicKey Публичный ключ узла (32 байта).
     */
    fun bootstrap(address: String, port: Int, publicKey: ByteArray) = synchronized(this) {
        nativeTox.toxBootstrap(toxPtr, address, port, publicKey)
        nativeTox.toxAddTcpRelay(toxPtr, address, port, publicKey)
    }

    /**
     * Корректно завершает работу нативного ядра и мультимедийного AV-моста,
     * высвобождая всю занятую нативную память.
     */
    fun stop() = synchronized(this) {
        nativeToxAv.toxavKill(toxavPtr)
        nativeTox.toxKill(toxPtr)
        toxavPtr = 0
        toxPtr = 0
        Log.i(TAG, "Killed Tox")
    }

    /**
     * Запускает одну итерацию обработки сетевых событий главного ядра Tox.
     */
    fun iterate(): Unit = synchronized(this) { nativeTox.toxIterate(toxPtr, eventListener) }

    /**
     * Запускает одну итерацию обработки аудио/видео кадров мультимедийного движка ToxAV.
     */
    fun iterateAv(): Unit = synchronized(this) { nativeToxAv.toxavIterate(toxavPtr, avEventListener) }

    /**
     * Возвращает рекомендуемый интервал времени в миллисекундах до следующего вызова [iterate].
     */
    fun iterationInterval(): Long = synchronized(this) { nativeTox.toxIterationInterval(toxPtr).toLong() }

    /**
     * Возвращает рекомендуемый интервал времени в миллисекундах до следующего вызова [iterateAv].
     */
    fun iterationIntervalAv(): Long = synchronized(this) { nativeToxAv.toxavIterationInterval(toxavPtr).toLong() }

    /**
     * Возвращает установленное имя пользователя текущего профиля.
     */
    fun getName(): String = synchronized(this) { String(nativeTox.toxGetName(toxPtr)) }

    /**
     * Устанавливает новое имя пользователя для текущего профиля.
     */
    fun setName(name: String) = synchronized(this) {
        nativeTox.toxSetName(toxPtr, name.toByteArray())
    }

    /**
     * Возвращает текущее статус-сообщение профиля.
     */
    fun getStatusMessage(): String = synchronized(this) { String(nativeTox.toxGetStatusMessage(toxPtr)) }

    /**
     * Устанавливает новое статус-сообщение для профиля.
     */
    fun setStatusMessage(statusMessage: String) = synchronized(this) {
        nativeTox.toxSetStatusMessage(toxPtr, statusMessage.toByteArray())
    }

    /**
     * Возвращает полный уникальный 38-байтовый Tox ID локального пользователя.
     */
    fun getToxId() = synchronized(this) { ToxID.fromBytes(nativeTox.toxGetAddress(toxPtr)) }

    /**
     * Возвращает 32-байтовый публичный криптографический ключ локального пользователя.
     */
    fun getPublicKey() = synchronized(this) { PublicKey.fromBytes(nativeTox.toxGetPublicKey(toxPtr)) }

    /**
     * Считывает текущее значение 32-битного nospam-кода профиля.
     */
    fun getNospam(): Int = synchronized(this) { nativeTox.toxGetNospam(toxPtr) }

    /**
     * Записывает новое значение nospam-кода для изменения хвоста Tox ID.
     */
    fun setNospam(value: Int) = synchronized(this) {
        nativeTox.toxSetNospam(toxPtr, value)
    }

    /**
     * Возвращает сериализованные бинарные данные всего профиля для записи файла сохранения.
     */
    fun getSaveData() = synchronized(this) { nativeTox.toxGetSavedata(toxPtr) }

    /**
     * Отправляет запрос на добавление в друзья новому контакту по его полному Tox ID.
     *
     * @param toxId Полный адрес контакта.
     * @param message Приветственное сообщение.
     */
    fun addContact(toxId: ToxID, message: String) = synchronized(this) {
        nativeTox.toxAddFriend(toxPtr, toxId.bytes(), message.toByteArray())
        updateContactMapping()
    }

    /**
     * Удаляет друга из нашего контакт-листа по его публичному ключу.
     */
    fun deleteContact(pk: PublicKey) = synchronized(this) {
        Log.i(TAG, "Deleting ${pk.fingerprint()}")
        val friendNumber = nativeTox.toxFriendByPublicKey(toxPtr, pk.bytes())
        if (friendNumber != -1) {
            nativeTox.toxDeleteFriend(toxPtr, friendNumber)
        } else {
            Log.e(TAG, "Tried to delete nonexistent contact, this can happen if the database is out of sync with the Tox save")
        }
        updateContactMapping()
    }

    /**
     * Возвращает список всех друзей в формате пар (Публичный ключ, Внутренний нативный ID).
     */
    fun getContacts(): List<Pair<PublicKey, Int>> = synchronized(this) {
        val friendNumbers = nativeTox.toxGetFriendList(toxPtr)
        Log.i(TAG, "Loading ${friendNumbers.size} friends")
        List(friendNumbers.size) {
            Pair(PublicKey.fromBytes(nativeTox.toxGetFriendPublicKey(toxPtr, friendNumbers[it])), friendNumbers[it])
        }
    }

    fun getFriendPublicKey(friendNumber: Int): ByteArray? = synchronized(this) {
        try {
            nativeTox.toxGetFriendPublicKey(toxPtr, friendNumber)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Отправляет приватное текстовое сообщение другу.
     *
     * @param publicKey Публичный ключ получателя.
     * @param message Текст сообщения.
     * @param type Тип сообщения (обычное или статус действия).
     * @return Внутренний идентификатор сообщения в очереди отправки.
     */
    fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int = synchronized(this) {
        nativeTox.toxFriendSendMessage(
            toxPtr,
            contactByKey(publicKey),
            type.toToxType().ordinal,
            message.toByteArray(),
        )
    }

    /**
     * Принимает входящий запрос в друзья от контакта, добавляя его без отсылки запроса.
     */
    fun acceptFriendRequest(pk: PublicKey) = synchronized(this) {
        try {
            nativeTox.toxAddFriendNorequest(toxPtr, pk.bytes())
            updateContactMapping()
        } catch (e: Exception) {
            Log.e(TAG, "Exception while accepting friend request $pk: $e")
        }
    }

    /**
     * Добавляет друга в список без отправки запроса (по publicKey).
     * Используется для временного добавления участников группы для bootstrap-поиска.
     * @return friendNumber, либо -1 при ошибке.
     */
    fun addFriendNoRequest(pk: PublicKey): Int = synchronized(this) {
        try {
            val result = nativeTox.toxAddFriendNorequest(toxPtr, pk.bytes())
            if (result >= 0) {
                updateContactMapping()
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception while adding friend norequest $pk: $e")
            -1
        }
    }

    /**
     * Возобновляет или одобряет входящий файловый трансфер.
     */
    fun startFileTransfer(pk: PublicKey, fileNumber: Int) = synchronized(this) {
        try {
            nativeTox.toxFileControl(toxPtr, contactByKey(pk), fileNumber, ToxFileControl.RESUME.ordinal)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ft ${pk.fingerprint()} $fileNumber\n$e")
        }
    }

    /**
     * Ставит на паузу или отменяет файловый трансфер.
     */
    fun stopFileTransfer(pk: PublicKey, fileNumber: Int) = synchronized(this) {
        try {
            nativeTox.toxFileControl(toxPtr, contactByKey(pk), fileNumber, ToxFileControl.CANCEL.ordinal)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ft ${pk.fingerprint()} $fileNumber\n$e")
        }
    }

    /**
     * Инициирует отправку нового файла другу.
     *
     * @param pk Публичный ключ получателя.
     * @param fileKind Назначение отправляемого файла (обычный файл или картинка аватарки).
     * @param fileSize Общий размер файла в байтах.
     * @param fileName Имя отправляемого файла.
     * @return Внутренний номер созданной файловой сессии.
     */
    fun sendFile(pk: PublicKey, fileKind: FileKind, fileSize: Long, fileName: String): Int = synchronized(this) {
        try {
            nativeTox.toxFileSend(toxPtr, contactByKey(pk), fileKind.toToxtype(), fileSize, Random.nextBytes(FILE_ID_LENGTH), fileName.toByteArray())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending ft $fileName ${pk.fingerprint()}\n$e")
            -1
        }
    }

    /**
     * Передает конкретный порционный блок байтов (чанк) отправляемого файла.
     */
    fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> = synchronized(this) {
        try {
            nativeTox.toxFileSendChunk(toxPtr, contactByKey(pk), fileNo, pos, data)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending chunk $pos:${data.size} to ${pk.fingerprint()} $fileNo\n$e")
            Result.failure(e)
        }
    }

    /**
     * Уведомляет контакта о статусе набора текста (печатает / закончил ввод).
     */
    fun setTyping(publicKey: PublicKey, typing: Boolean) = synchronized(this) {
        nativeTox.toxSetTyping(toxPtr, contactByKey(publicKey), typing)
    }

    /**
     * Возвращает наш текущий сетевой статус доступности.
     */
    fun getStatus() = synchronized(this) { UserStatus.entries[nativeTox.toxGetSelfUserStatus(toxPtr)] }

    /**
     * Устанавливает новый статус доступности локального профиля.
     */
    fun setStatus(status: UserStatus) = synchronized(this) {
        nativeTox.toxSetSelfUserStatus(toxPtr, status.toToxType().ordinal)
    }

    /**
     * Отправляет гарантированный P2P-пакет пользовательских данных другу.
     */
    fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): CustomPacketError = synchronized(this) {
        try {
            nativeTox.toxFriendSendLosslessPacket(toxPtr, contactByKey(pk), packet)
            CustomPacketError.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending lossless packet: $e")
            CustomPacketError.Invalid
        }
    }

    /**
     * Отправляет кастомный ненадежный lossy-пакет другу.
     */
    fun sendLossyPacket(pk: PublicKey, data: ByteArray): CustomPacketError = synchronized(this) {
        try {
            nativeTox.toxFriendSendLossyPacket(toxPtr, contactByKey(pk), data)
            CustomPacketError.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending lossy packet: $e")
            CustomPacketError.Invalid
        }
    }

    /**
     * Возвращает секретный (приватный) ключ нашего профиля (32 байта).
     */
    fun selfGetSecretKey(): ByteArray = synchronized(this) {
        nativeTox.toxSelfGetSecretKey(toxPtr)
    }

    /**
     * Возвращает активный UDP-порт нашего локального узла.
     */
    fun selfGetUdpPort(): Int = synchronized(this) {
        nativeTox.toxSelfGetUdpPort(toxPtr)
    }

    /**
     * Возвращает активный TCP-порт нашего локального узла.
     */
    fun selfGetTcpPort(): Int = synchronized(this) {
        nativeTox.toxSelfGetTcpPort(toxPtr)
    }

    /**
     * Возвращает временный DHT-ключ (DHT ID) нашего инстанса (32 байта).
     */
    fun selfGetDhtId(): ByteArray = synchronized(this) {
        nativeTox.toxSelfGetDhtId(toxPtr)
    }

    /**
     * Возвращает UNIX-время последнего зафиксированного присутствия друга на связи.
     */
    fun friendGetLastOnline(pk: PublicKey): Long = synchronized(this) {
        nativeTox.toxFriendGetLastOnline(toxPtr, contactByKey(pk))
    }

    /**
     * Проверяет, вводит ли в данный момент друг сообщение в чате (активный ввод).
     */
    fun friendGetTyping(pk: PublicKey): Boolean = synchronized(this) {
        nativeTox.toxFriendGetTyping(toxPtr, contactByKey(pk))
    }

    /**
     * Вычисляет внутренний нативный ID друга по его публичному криптографическому ключу.
     */
    private fun contactByKey(pk: PublicKey): Int = synchronized(this) {
        nativeTox.toxFriendByPublicKey(toxPtr, pk.bytes())
    }

    /**
     * Возвращает нативный номер друга по его публичному ключу.
     */
    fun getFriendNumberByPublicKey(pk: PublicKey): Int = synchronized(this) {
        nativeTox.toxFriendByPublicKey(toxPtr, pk.bytes())
    }

    /**
     * Инициирует исходящий 1-на-1 голосовой вызов другу.
     */
    fun startCall(pk: PublicKey) = synchronized(this) {
        nativeToxAv.toxavCall(toxavPtr, contactByKey(pk), audioBitrate, 0)
    }

    /**
     * Одобряет и подключается к входящему голосовому вызову от друга.
     */
    fun answerCall(pk: PublicKey) = synchronized(this) {
        nativeToxAv.toxavAnswer(toxavPtr, contactByKey(pk), audioBitrate, 0)
    }

    /**
     * Прерывает, сбрасывает или отклоняет текущий вызов.
     */
    fun endCall(pk: PublicKey) = synchronized(this) {
        nativeToxAv.toxavCallControl(toxavPtr, contactByKey(pk), ToxavCallControl.CANCEL.ordinal)
    }

    /**
     * Передает порционный PCM-кадр записанного голоса собеседнику.
     */
    fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int) = synchronized(this) {
        nativeToxAv.toxavAudioSendFrame(toxavPtr, contactByKey(pk), pcm, pcm.size, channels, samplingRate)
    }

    /**
     * Отправляет видеокадр YUV420P собеседнику в рамках видеовызова.
     */
    fun sendVideoFrame(pk: PublicKey, width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray): Boolean = synchronized(this) {
        nativeToxAv.toxavVideoSendFrame(toxavPtr, contactByKey(pk), width, height, y, u, v)
    }

    /**
     * Изменяет битрейт аудио потока на лету во время звонка.
     */
    fun audioSetBitRate(pk: PublicKey, bitrate: Int): Boolean = synchronized(this) {
        nativeToxAv.toxavAudioSetBitRate(toxavPtr, contactByKey(pk), bitrate)
    }

    /**
     * Изменяет битрейт видео потока на лету во время звонка.
     */
    fun videoSetBitRate(pk: PublicKey, bitrate: Int): Boolean = synchronized(this) {
        nativeToxAv.toxavVideoSetBitRate(toxavPtr, contactByKey(pk), bitrate)
    }

    /**
     * Создает новую групповую NGC-конференцию.
     *
     * @param privacyState Вид приватности (публичная или приватная).
     * @param groupName Название создаваемой группы.
     * @param selfName Наш псевдоним внутри создаваемой группы.
     */
    fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int = synchronized(this) {
        nativeTox.toxGroupNew(toxPtr, privacyState.value, groupName, selfName)
    }

    /**
     * Присоединяется к групповой NGC-конференции по полученным пригласительным данным.
     */
    fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int = synchronized(this) {
        nativeTox.toxGroupJoin(toxPtr, friendNo, inviteData, selfName, password)
    }

    /**
     * Выходит из состава участников указанной NGC-группы.
     */
    fun groupLeave(groupNumber: Int): Boolean = synchronized(this) {
        nativeTox.toxGroupLeave(toxPtr, groupNumber)
    }

    /**
     * Отправляет групповое сообщение всем участникам NGC-конференции.
     */
    fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int = synchronized(this) {
        nativeTox.toxGroupSendMessage(toxPtr, groupNumber, type.ordinal, message)
    }

    /**
     * Устанавливает тему для указанной NGC-группы.
     */
    fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean = synchronized(this) {
        nativeTox.toxGroupSetTopic(toxPtr, groupNumber, topic)
    }

    /**
     * Получает текущую тему указанной NGC-группы.
     */
    fun groupGetTopic(groupNumber: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupGetTopic(toxPtr, groupNumber)
    }

    /**
     * Получает название указанной NGC-группы.
     */
    fun groupGetName(groupNumber: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupGetName(toxPtr, groupNumber)
    }

    /**
     * Возвращает уникальный постоянный 32-байтовый ID группового чата NGC.
     */
    fun groupGetChatId(groupNumber: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupGetChatId(toxPtr, groupNumber)
    }

    /**
     * Устанавливает или сбрасывает пароль доступа к NGC-группе.
     */
    fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean = synchronized(this) {
        nativeTox.toxGroupSetPassword(toxPtr, groupNumber, password)
    }

    /**
     * Получает текущий пароль для входа в данную NGC-группу.
     */
    fun groupGetPassword(groupNumber: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupGetPassword(toxPtr, groupNumber)
    }

    /**
     * Получает имя конкретного участника NGC-группы по его ID.
     */
    fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupPeerGetName(toxPtr, groupNumber, peerId)
    }

    /**
     * Получает публичный ключ конкретного участника NGC-группы по его ID.
     */
    fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? = synchronized(this) {
        nativeTox.toxGroupPeerGetPublicKey(toxPtr, groupNumber, peerId)
    }

    /**
     * Возвращает наш собственный ID участника в NGC-группе.
     */
    fun groupSelfGetPeerId(groupNumber: Int): Int = synchronized(this) {
        nativeTox.toxGroupSelfGetPeerId(toxPtr, groupNumber)
    }

    /**
     * Возвращает нашу текущую роль внутри NGC-группы.
     */
    fun groupSelfGetRole(groupNumber: Int): ToxGroupRole = synchronized(this) {
        ToxGroupRole.fromInt(nativeTox.toxGroupSelfGetRole(toxPtr, groupNumber))
    }

    /**
     * Отправляет приглашение в NGC-группу конкретному другу.
     */
    fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean = synchronized(this) {
        nativeTox.toxGroupInviteSend(toxPtr, groupNumber, friendNumber)
    }

    /**
     * Присоединяется к NGC-группе напрямую по Chat ID.
     */
    fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int = synchronized(this) {
        nativeTox.toxGroupJoinDirect(toxPtr, chatId, selfName, password)
    }

    /**
     * Переподключается к ранее сохранённой NGC-группе после загрузки профиля.
     */
    fun groupReconnect(groupNumber: Int): Boolean = synchronized(this) {
        nativeTox.toxGroupReconnect(toxPtr, groupNumber)
    }

    /**
     * Возвращает массив groupNumber всех активных NGC-групп в текущей сессии.
     */
    fun groupGetChatlist(): IntArray = synchronized(this) {
        nativeTox.toxGroupGetChatlist(toxPtr)
    }

    /**
     * Создает групповую аудио-конференцию на базе группы.
     */
    fun groupavAdd(): Int = synchronized(this) {
        nativeToxAv.toxavAddAvGroupchat(toxPtr)
    }

    /**
     * Подключается к групповой аудио-конференции.
     */
    fun groupavJoin(groupNumber: Int): Int = synchronized(this) {
        nativeToxAv.toxavJoinAvGroupchat(toxPtr, groupNumber)
    }

    /**
     * Транслирует PCM-кадр нашего голоса всем участникам групповой конференции.
     */
    fun groupavSendAudio(groupNumber: Int, pcm: ShortArray, channels: Int, samplingRate: Int): Int = synchronized(this) {
        nativeToxAv.toxavGroupSendAudio(toxPtr, groupNumber, pcm, pcm.size, channels, samplingRate)
    }

    /**
     * Активирует функции аудио/видео вызова в групповом чате.
     */
    fun groupavEnableAudio(groupNumber: Int): Int = synchronized(this) {
        nativeToxAv.toxavGroupchatEnableAv(toxPtr, groupNumber)
    }

    /**
     * Выключает функции аудио/видео вызова в групповом чате.
     */
    fun groupavDisableAudio(groupNumber: Int): Int = synchronized(this) {
        nativeToxAv.toxavGroupchatDisableAv(toxPtr, groupNumber)
    }

    /**
     * Проверяет, активна ли трансляция звука в данном групповом чате.
     */
    fun groupavIsEnabled(groupNumber: Int): Boolean = synchronized(this) {
        nativeToxAv.toxavGroupchatAvEnabled(toxPtr, groupNumber)
    }
}
