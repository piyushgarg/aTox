package ltd.evilcorp.core.tox

import ltd.evilcorp.domain.model.UserStatus
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.domain.tox.enums.ToxFileControl
import ltd.evilcorp.domain.tox.enums.ToxFileKind
import ltd.evilcorp.domain.tox.enums.ToxMessageType

class NativeTox {
    init {
        System.loadLibrary("nativetox")
    }

    /**
     * Создает и инициализирует новый нативный инстанс Tox.
     * @param savedata Сохраненные ранее байтовые данные профиля (tox save), либо null для создания нового аккаунта.
     * @return Указатель (pointer) на нативную структуру Tox в памяти.
     */
    external fun toxNew(savedata: ByteArray?): Long

    /**
     * Создает и инициализирует новый нативный инстанс Tox с полным набором сетевых настроек и поддержкой прокси.
     * @param savedata Сохраненные ранее байтовые данные профиля (tox save), либо null.
     * @param ipv6Enabled true для включения IPv6.
     * @param udpEnabled true для включения UDP (прямые соединения), false для принудительного TCP-режима.
     * @param localDiscoveryEnabled true для обнаружения узлов в локальной Wi-Fi/LAN сети.
     * @param proxyType Тип прокси (0 - без прокси, 1 - HTTP, 2 - SOCKS5).
     * @param proxyHost Хост прокси-сервера (например, "127.0.0.1" для локального Tor-демона), либо null.
     * @param proxyPort Порт прокси-сервера (например, 9050 для Tor).
     * @return Указатель (pointer) на нативную структуру Tox в памяти, либо 0 в случае ошибки.
     */
    external fun toxNewWithOptions(
        savedata: ByteArray?,
        ipv6Enabled: Boolean,
        udpEnabled: Boolean,
        localDiscoveryEnabled: Boolean,
        proxyType: Int,
        proxyHost: String?,
        proxyPort: Int
    ): Long

    /**
     * Уничтожает инстанс Tox и освобождает всю связанную с ним нативную память.
     * @param tox Указатель на инстанс Tox.
     */
    external fun toxKill(tox: Long)
    
    /**
     * Подключает инстанс к публичному DHT-узлу (bootstrap) сети Tox.
     * @param tox Указатель на инстанс Tox.
     * @param address IP-адрес или доменное имя DHT-узла.
     * @param port Порт DHT-узла.
     * @param publicKey Публичный ключ DHT-узла (32 байта).
     */
    external fun toxBootstrap(tox: Long, address: String, port: Int, publicKey: ByteArray)

    /**
     * Добавляет TCP-релей для обхода сложных NAT и фаерволов.
     * @param tox Указатель на инстанс Tox.
     * @param address IP-адрес или доменное имя TCP-релея.
     * @param port Порт TCP-релея.
     * @param publicKey Публичный ключ TCP-релея (32 байта).
     */
    external fun toxAddTcpRelay(tox: Long, address: String, port: Int, publicKey: ByteArray)
    
    /**
     * Главный рабочий цикл Tox: обрабатывает входящие/исходящие сетевые пакеты и вызывает зарегистрированные коллбеки.
     * Должен вызываться регулярно в цикле.
     * @param tox Указатель на инстанс Tox.
     * @param listener Реализация слушателя событий для проброса событий в Kotlin.
     */
    external fun toxIterate(tox: Long, listener: ToxEventListener)

    /**
     * Возвращает рекомендуемый интервал времени в миллисекундах до следующего вызова [toxIterate].
     * @param tox Указатель на инстанс Tox.
     * @return Интервал в миллисекундах.
     */
    external fun toxIterationInterval(tox: Long): Int

    /**
     * Возвращает текущее имя нашего профиля в Tox.
     * @param tox Указатель на инстанс Tox.
     * @return Имя профиля в виде байтового массива (UTF-8).
     */
    external fun toxGetName(tox: Long): ByteArray

    /**
     * Устанавливает новое имя для нашего профиля в Tox.
     * @param tox Указатель на инстанс Tox.
     * @param name Новое имя в виде байтового массива (UTF-8).
     */
    external fun toxSetName(tox: Long, name: ByteArray)

    /**
     * Возвращает текущее статус-сообщение нашего профиля.
     * @param tox Указатель на инстанс Tox.
     * @return Текст статуса в виде байтового массива (UTF-8).
     */
    external fun toxGetStatusMessage(tox: Long): ByteArray

    /**
     * Устанавливает новое статус-сообщение для нашего профиля.
     * @param tox Указатель на инстанс Tox.
     * @param msg Текст нового статуса в виде байтового массива (UTF-8).
     */
    external fun toxSetStatusMessage(tox: Long, msg: ByteArray)
    
    /**
     * Возвращает полный уникальный Tox ID нашего аккаунта (38 байт: 32 байта публичный ключ + 4 байта nospam + 2 байта checksum).
     * @param tox Указатель на инстанс Tox.
     * @return Полный Tox-адрес в виде байтового массива.
     */
    external fun toxGetAddress(tox: Long): ByteArray

    /**
     * Возвращает только публичный ключ (Tox Public Key) нашего аккаунта (32 байта).
     * @param tox Указатель на инстанс Tox.
     * @return Публичный ключ в виде байтового массива.
     */
    external fun toxGetPublicKey(tox: Long): ByteArray

    /**
     * Возвращает секретный (приватный) ключ нашего аккаунта (32 байта).
     * ВНИМАНИЕ: Секретный ключ должен храниться в строжайшем секрете!
     * @param tox Указатель на инстанс Tox.
     * @return Секретный ключ в виде байтового массива.
     */
    external fun toxSelfGetSecretKey(tox: Long): ByteArray

    /**
     * Возвращает активный UDP-порт нашего запущенного узла.
     * @param tox Указатель на инстанс Tox.
     * @return Номер порта, либо 0 в случае ошибки или если UDP выключен.
     */
    external fun toxSelfGetUdpPort(tox: Long): Int

    /**
     * Возвращает активный TCP-порт нашего запущенного узла.
     * @param tox Указатель на инстанс Tox.
     * @return Номер порта, либо 0 в случае ошибки.
     */
    external fun toxSelfGetTcpPort(tox: Long): Int

    /**
     * Возвращает временный DHT-ключ (DHT ID) нашего инстанса (32 байта).
     * Используется для диагностики подключения к глобальной сети.
     * @param tox Указатель на инстанс Tox.
     * @return DHT ID в виде байтового массива.
     */
    external fun toxSelfGetDhtId(tox: Long): ByteArray
    
    /**
     * Возвращает текущее значение Nospam нашего профиля (используется для предотвращения спама).
     * @param tox Указатель на инстанс Tox.
     * @return 32-битное число nospam.
     */
    external fun toxGetNospam(tox: Long): Int

    /**
     * Устанавливает новое значение Nospam для генерации нового Tox ID.
     * @param tox Указатель на инстанс Tox.
     * @param nospam Новое 32-битное число.
     */
    external fun toxSetNospam(tox: Long, nospam: Int)
    
    /**
     * Сериализует текущее состояние профиля Tox (список друзей, имя, настройки) в байты для последующего сохранения.
     * @param tox Указатель на инстанс Tox.
     * @return Байтовый массив сохраненного состояния.
     */
    external fun toxGetSavedata(tox: Long): ByteArray

    /**
     * Отправляет запрос на добавление в друзья.
     * @param tox Указатель на инстанс Tox.
     * @param pubKey Публичный ключ друга (32 байта).
     * @param message Приветственное текстовое сообщение (UTF-8).
     * @return Номер созданного друга (ID друга), либо код ошибки (отрицательное число).
     */
    external fun toxAddFriend(tox: Long, pubKey: ByteArray, message: ByteArray): Int

    /**
     * Добавляет друга в список друзей без отправки запроса (например, при подтверждении или импорте).
     * @param tox Указатель на инстанс Tox.
     * @param pubKey Публичный ключ друга (32 байта).
     * @return Номер созданного друга (ID друга), либо код ошибки.
     */
    external fun toxAddFriendNorequest(tox: Long, pubKey: ByteArray): Int

    /**
     * Удаляет друга из списка контактов.
     * @param tox Указатель на инстанс Tox.
     * @param friendNumber Номер удаляемого друга.
     */
    external fun toxDeleteFriend(tox: Long, friendNumber: Int)
    
    /**
     * Возвращает список идентификаторов (номеров) всех друзей в нашем контакт-листе.
     * @param tox Указатель на инстанс Tox.
     * @return Массив идентификаторов друзей.
     */
    external fun toxGetFriendList(tox: Long): IntArray

    /**
     * Возвращает публичный ключ друга по его номеру.
     * @param tox Указатель на инстанс Tox.
     * @param friendNumber Номер друга.
     * @return Публичный ключ друга (32 байта).
     */
    external fun toxGetFriendPublicKey(tox: Long, friendNumber: Int): ByteArray

    /**
     * Находит номер друга в контакт-листе по его публичному ключу.
     * @param tox Указатель на инстанс Tox.
     * @param pubKey Публичный ключ (32 байта).
     * @return Номер друга в контакт-листе, либо -1, если друг не найден.
     */
    external fun toxFriendByPublicKey(tox: Long, pubKey: ByteArray): Int

    /**
     * Проверяет, существует ли друг с указанным номером в нашем списке контактов.
     * @param tox Указатель на инстанс Tox.
     * @param friendNumber Номер друга.
     * @return true, если друг существует, false в противном случае.
     */
    external fun toxFriendExists(tox: Long, friendNumber: Int): Boolean

    /**
     * Возвращает текущее имя друга по его номеру (прямой запрос).
     * @param tox Указатель на инстанс Tox.
     * @param friendNumber Номер друга.
     * @return Имя друга в виде байтового массива (UTF-8).
     */
    external fun toxFriendGetName(tox: Long, friendNumber: Int): ByteArray

    /**
     * Возвращает статус-сообщение друга по его номеру (прямой запрос).
     * @param tox Указатель на инстанс Tox.
     * @param friendNumber Номер друга.
     * @return Статус-сообщение в виде байтового массива (UTF-8).
     */
    external fun toxFriendGetStatusMessage(tox: Long, friendNumber: Int): ByteArray

    /**
     * Возвращает онлайн-статус друга (Away/Busy/Online) по его номеру (прямой запрос).
     * @param tox Указатель на инстанс Tox.
     * @param friendNumber Номер друга.
     * @return 0 - в сети, 1 - отошел, 2 - не беспокоить.
     */
    external fun toxFriendGetStatus(tox: Long, friendNumber: Int): Int

    /**
     * Возвращает текущий тип сетевого соединения с другом (прямой запрос).
     * @param tox Указатель на инстанс Tox.
     * @param friendNumber Номер друга.
     * @return 0 - офлайн, 1 - TCP-соединение, 2 - UDP-соединение.
     */
    external fun toxFriendGetConnectionStatus(tox: Long, friendNumber: Int): Int

    /**
     * Проверяет, пишет ли в данный момент друг нам сообщение (прямой запрос).
     * @param tox Указатель на инстанс Tox.
     * @param friendNumber Номер друга.
     * @return true, если друг печатает, false в противном случае.
     */
    external fun toxFriendGetTyping(tox: Long, friendNumber: Int): Boolean

    /**
     * Возвращает UNIX-время последнего зафиксированного онлайн-присутствия друга.
     * @param tox Указатель на инстанс Tox.
     * @param friendNumber Номер друга.
     * @return UNIX timestamp (в секундах), либо 0, если друг в сети прямо сейчас или никогда не подключался.
     */
    external fun toxFriendGetLastOnline(tox: Long, friendNumber: Int): Long
    
    /**
     * Отправляет приватное текстовое сообщение другу.
     * @param tox Указатель на инстанс Tox.
     * @param friendNumber Номер друга.
     * @param type Тип сообщения (0 - обычное, 1 - action).
     * @param message Текст сообщения в виде байтового массива (UTF-8).
     * @return Уникальный ID отправленного сообщения, либо 0 в случае ошибки.
     */
    external fun toxFriendSendMessage(tox: Long, friendNumber: Int, type: Int, message: ByteArray): Int

    /**
     * Уведомляет друга о том, печатаем мы сообщение в данный момент или нет.
     * @param tox Указатель на инстанс Tox.
     * @param friendNumber Номер друга.
     * @param typing true, если мы печатаем, false в противном случае.
     */
    external fun toxSetTyping(tox: Long, friendNumber: Int, typing: Boolean)
    
    /**
     * Возвращает текущий онлайн-статус нашего профиля (0 - в сети, 1 - отошел, 2 - не беспокоить).
     * @param tox Указатель на инстанс Tox.
     * @return Код статуса.
     */
    external fun toxGetSelfUserStatus(tox: Long): Int

    /**
     * Устанавливает новый онлайн-статус для нашего профиля.
     * @param tox Указатель на инстанс Tox.
     * @param status Код статуса (0 - в сети, 1 - отошел, 2 - не беспокоить).
     */
    external fun toxSetSelfUserStatus(tox: Long, status: Int)
 
    /**
     * Управляет текущей передачей файла (пауза, возобновление, отмена).
     * @param tox Указатель на инстанс Tox.
     * @param friendNumber Номер друга.
     * @param fileNumber Номер файла.
     * @param control Код управления (например, 0 - приостановить, 1 - продолжить, 2 - отменить).
     */
    external fun toxFileControl(tox: Long, friendNumber: Int, fileNumber: Int, control: Int)

    /**
     * Начинает отправку файла другу.
     * @param tox Указатель на инстанс Tox.
     * @param friendNumber Номер друга.
     * @param kind Тип файла (0 - обычный файл, 1 - аватарка).
     * @param fileSize Общий размер файла в байтах.
     * @param fileId Уникальный ID файла (32 байта).
     * @param filename Имя файла в виде байтового массива.
     * @return Уникальный номер отправляемого файла в рамках сессии, либо -1 при ошибке.
     */
    external fun toxFileSend(tox: Long, friendNumber: Int, kind: Int, fileSize: Long, fileId: ByteArray, filename: ByteArray): Int

    /**
     * Отправляет конкретный чанк (кусок) файла.
     * @param tox Указатель на инстанс Tox.
     * @param friendNumber Номер друга.
     * @param fileNumber Номер файла.
     * @param position Позиция чанка в файле (смещение в байтах).
     * @param data Байты чанка.
     */
    external fun toxFileSendChunk(tox: Long, friendNumber: Int, fileNumber: Int, position: Long, data: ByteArray)

    /**
     * Возвращает уникальный 32-байтовый ID передаваемого или принимаемого файла.
     * @param tox Указатель на инстанс Tox.
     * @param friendNumber Номер друга.
     * @param fileNumber Номер файла.
     * @return 32-байтовый ID файла, либо null в случае ошибки.
     */
    external fun toxFileGetFileId(tox: Long, friendNumber: Int, fileNumber: Int): ByteArray

    /**
     * Отправляет кастомный lossless-пакет данных другу (используется для передачи служебных данных).
     * @param tox Указатель на нативный инстанс Tox.
     * @param friendNumber Номер друга.
     * @param data Байты пакета.
     */
    external fun toxFriendSendLosslessPacket(tox: Long, friendNumber: Int, data: ByteArray)

    /**
     * Отправляет кастомный ненадежный lossy-пакет данных другу (используется для быстрых некритичных данных).
     * @param tox Указатель на нативный инстанс Tox.
     * @param friendNumber Номер друга.
     * @param data Байты пакета.
     */
    external fun toxFriendSendLossyPacket(tox: Long, friendNumber: Int, data: ByteArray)

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
     * Устанавливает новое название (заголовок) для группового чата.
     * @param tox Указатель на нативный инстанс Tox.
     * @param conferenceNumber Номер конференции.
     * @param title Новое название в виде байтового массива (UTF-8).
     */
    external fun toxConferenceSetTitle(tox: Long, conferenceNumber: Int, title: ByteArray)

    /**
     * Возвращает текущее название (заголовок) группового чата.
     * @param tox Указатель на нативный инстанс Tox.
     * @param conferenceNumber Номер конференции.
     * @return Название в виде байтового массива (UTF-8).
     */
    external fun toxConferenceGetTitle(tox: Long, conferenceNumber: Int): ByteArray

    /**
     * Проверяет, является ли участник конференции с указанным номером нами самими.
     * @param tox Указатель на нативный инстанс Tox.
     * @param conferenceNumber Номер конференции.
     * @param peerNumber Номер участника.
     * @return true, если участник - это мы, false в противном случае.
     */
    external fun toxConferencePeerNumberIsOurself(tox: Long, conferenceNumber: Int, peerNumber: Int): Boolean

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

    // ===================================================================================
    // Новые Групповые конференции NGC (Next Generation Conferences / Tox Groups)
    // ===================================================================================

    /**
     * Создает новую групповую NGC-конференцию.
     * @param tox Указатель на нативный инстанс Tox.
     * @param privacyState Статус приватности группы (0 - Public, 1 - Private).
     * @param groupName Название создаваемой группы в виде байтового массива.
     * @param selfName Ваше имя в создаваемой группе.
     * @return Уникальный номер группы в Tox (Tox_Group_Number), либо -1 в случае ошибки.
     */
    external fun toxGroupNew(tox: Long, privacyState: Int, groupName: ByteArray, selfName: ByteArray): Int

    /**
     * Присоединяется к групповой NGC-конференции по полученному приглашению.
     * @param tox Указатель на нативный инстанс Tox.
     * @param friendNumber Номер друга, приславшего приглашение.
     * @param inviteData Массив байт данных приглашения (invite_data).
     * @param selfName Ваше имя при входе в группу.
     * @param password Пароль группы (если есть, иначе null или пустой массив).
     * @return Номер присоединенной группы, либо -1 в случае ошибки.
     */
    external fun toxGroupJoin(tox: Long, friendNumber: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int

    /**
     * Выходит из групповой NGC-конференции.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @return true в случае успеха, false в случае ошибки.
     */
    external fun toxGroupLeave(tox: Long, groupNumber: Int): Boolean

    /**
     * Отправляет текстовое сообщение в NGC-группу.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @param type Тип сообщения (0 - Normal, 1 - Action /me).
     * @param message Текст сообщения в виде байтового массива.
     * @return Уникальный ID отправленного сообщения в рамках группы, либо -1 в случае ошибки.
     */
    external fun toxGroupSendMessage(tox: Long, groupNumber: Int, type: Int, message: ByteArray): Int

    /**
     * Устанавливает тему (topic) для NGC-группы.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @param topic Новая тема в виде байтового массива.
     * @return true в случае успеха, false в случае ошибки.
     */
    external fun toxGroupSetTopic(tox: Long, groupNumber: Int, topic: ByteArray): Boolean

    /**
     * Получает текущую тему (topic) NGC-группы.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @return Массив байт темы, либо null в случае ошибки.
     */
    external fun toxGroupGetTopic(tox: Long, groupNumber: Int): ByteArray?

    /**
     * Получает название NGC-группы.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @return Массив байт названия, либо null в случае ошибки.
     */
    external fun toxGroupGetName(tox: Long, groupNumber: Int): ByteArray?

    /**
     * Возвращает уникальный постоянный 32-байтовый идентификатор NGC-чата (Chat ID).
     * Необходим для отслеживания и синхронизации групп между устройствами.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @return 32-байтовый Chat ID, либо null в случае ошибки.
     */
    external fun toxGroupGetChatId(tox: Long, groupNumber: Int): ByteArray?

    /**
     * Устанавливает пароль для доступа к NGC-группе.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @param password Пароль в виде байтового массива (null для удаления пароля).
     * @return true в случае успеха, false в случае ошибки.
     */
    external fun toxGroupSetPassword(tox: Long, groupNumber: Int, password: ByteArray?): Boolean

    /**
     * Возвращает текущий установленный пароль группы.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @return Массив байт пароля, либо null в случае ошибки или отсутствия пароля.
     */
    external fun toxGroupGetPassword(tox: Long, groupNumber: Int): ByteArray?

    /**
     * Получает имя участника NGC-группы по его ID.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @param peerId Внутренний идентификатор участника в группе.
     * @return Массив байт имени участника, либо null в случае ошибки.
     */
    external fun toxGroupPeerGetName(tox: Long, groupNumber: Int, peerId: Int): ByteArray?

    /**
     * Получает публичный ключ участника NGC-группы по его ID.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @param peerId Внутренний идентификатор участника в группе.
     * @return 32-байтовый публичный ключ участника, либо null в случае ошибки.
     */
    external fun toxGroupPeerGetPublicKey(tox: Long, groupNumber: Int, peerId: Int): ByteArray?

    /**
     * Возвращает наш собственный Peer ID в NGC-группе.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @return Наш Peer ID в группе, либо -1 в случае ошибки.
     */
    external fun toxGroupSelfGetPeerId(tox: Long, groupNumber: Int): Int

    /**
     * Возвращает нашу текущую роль в NGC-группе.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @return Роль (0 - Owner, 1 - Moderator, 2 - Participant), либо -1 в случае ошибки.
     */
    external fun toxGroupSelfGetRole(tox: Long, groupNumber: Int): Int

    /**
     * Отправляет приглашение в NGC-группу конкретному другу.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @param friendNumber Номер друга, которому отправляем приглашение.
     * @return true в случае успеха, false в случае ошибки.
     */
    external fun toxGroupInviteSend(tox: Long, groupNumber: Int, friendNumber: Int): Boolean

    /**
     * Присоединяется к NGC-группе напрямую по Chat ID (без инвайта от друга).
     * @param tox Указатель на нативный инстанс Tox.
     * @param chatId 32-байтовый Chat ID группы.
     * @param selfName Имя пользователя при входе в группу.
     * @param password Пароль группы (если есть, иначе null или пустой массив).
     * @return Номер присоединённой группы, либо -1 в случае ошибки.
     */
    external fun toxGroupJoinDirect(tox: Long, chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int

    /**
     * Переподключается к ранее сохранённой NGC-группе.
     * Должна вызываться после загрузки сохранённого профиля для каждой группы.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы, возвращённый при первом подключении.
     * @return true в случае успешного переподключения, false в случае ошибки.
     */
    external fun toxGroupReconnect(tox: Long, groupNumber: Int): Boolean

    // Раздел шифрования профилей Tox (Tox Encrypt / Decrypt API)

    /**
     * Извлекает соль (salt) из зашифрованных байтовых данных профиля для последующей генерации ключа.
     * @param data Зашифрованный массив байт профиля.
     * @return 32-байтовая соль, либо null в случае ошибки или если данные не зашифрованы.
     */
    external fun getSalt(data: ByteArray): ByteArray?

    /**
     * Генерирует криптографический ключ (passkey) на основе пароля и соли с использованием PBKDF2.
     * @param passphrase Пароль в виде байтового массива.
     * @param salt Соль в виде байтового массива.
     * @return Сгенерированный ключ (passkey), либо null в случае ошибки.
     */
    external fun passKeyDeriveWithSalt(passphrase: ByteArray, salt: ByteArray): ByteArray?

    /**
     * Расшифровывает байтовые данные профиля с использованием сгенерированного ключа (passkey).
     * @param data Зашифрованный массив байт профиля.
     * @param passkey Сгенерированный passkey ключ (из [passKeyDeriveWithSalt]).
     * @return Расшифрованный массив байт профиля, либо null в случае неверного ключа или ошибки.
     */
    external fun passDecrypt(data: ByteArray, passkey: ByteArray): ByteArray?

    /**
     * Зашифровывает байтовые данные профиля с использованием ключа (passkey).
     * @param data Исходные несжатые байты профиля.
     * @param passkey Сгенерированный passkey ключ.
     * @return Зашифрованный массив байт, готовый для сохранения, либо null в случае ошибки.
     */
    external fun passEncrypt(data: ByteArray, passkey: ByteArray): ByteArray?
}
