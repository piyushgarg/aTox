package ltd.evilcorp.core.tox.listener

import ltd.evilcorp.domain.tox.enums.ToxConnection
import ltd.evilcorp.domain.tox.enums.ToxFileControl
import ltd.evilcorp.domain.tox.enums.ToxMessageType
import ltd.evilcorp.domain.tox.enums.ToxUserStatus
import ltd.evilcorp.domain.tox.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.tox.enums.ToxGroupTopicLock
import ltd.evilcorp.domain.tox.enums.ToxGroupVoiceState
import ltd.evilcorp.domain.tox.enums.ToxGroupRole
import ltd.evilcorp.domain.tox.enums.ToxGroupExitType
import ltd.evilcorp.domain.tox.enums.ToxGroupJoinFail
import ltd.evilcorp.domain.tox.enums.ToxGroupModEvent
import javax.inject.Inject
import ltd.evilcorp.domain.model.ConnectionStatus
import ltd.evilcorp.domain.model.PublicKey
import ltd.evilcorp.domain.model.UserStatus
import ltd.evilcorp.domain.tox.bytesToHex

// Вызывается при получении кастомного lossless-пакета от друга
typealias FriendLosslessPacketHandler = (publicKey: String, data: ByteArray) -> Unit
// Вызывается при изменении состояния передачи файла (пауза, возобновление, отмена)
typealias FileRecvControlHandler = (publicKey: String, fileNo: Int, control: ToxFileControl) -> Unit
// Вызывается при изменении статус-сообщения друга
typealias FriendStatusMessageHandler = (publicKey: String, message: String) -> Unit
// Вызывается при подтверждении прочтения сообщения другом
typealias FriendReadReceiptHandler = (publicKey: String, messageId: Int) -> Unit
// Вызывается при изменении онлайн-статуса друга (в сети, отошел, не беспокоить)
typealias FriendStatusHandler = (publicKey: String, status: UserStatus) -> Unit
// Вызывается при изменении статуса подключения друга (онлайн/офлайн)
typealias FriendConnectionStatusHandler = (publicKey: String, status: ConnectionStatus) -> Unit
// Вызывается при получении нового входящего запроса на добавление в друзья
typealias FriendRequestHandler = (publicKey: String, timeDelta: Int, message: String) -> Unit
// Вызывается при получении нового приватного сообщения от друга
typealias FriendMessageHandler = (
    publicKey: String,
    messageType: ToxMessageType,
    timeDelta: Int,
    message: String,
) -> Unit
// Вызывается при изменении имени друга
typealias FriendNameHandler = (publicKey: String, newName: String) -> Unit
// Вызывается при получении очередного чанка (куска) файла
typealias FileRecvChunkHandler = (publicKey: String, fileNo: Int, position: Long, data: ByteArray) -> Unit
// Вызывается при получении запроса на прием файла от друга
typealias FileRecvHandler = (publicKey: String, fileNo: Int, kind: Int, size: Long, name: String) -> Unit
// Вызывается при получении кастомного lossy-пакета от друга
typealias FriendLossyPacketHandler = (publicKey: String, data: ByteArray) -> Unit
// Вызывается при изменении нашего собственного статуса подключения к сети DHT (подключен/отключен)
typealias SelfConnectionStatusHandler = (status: ConnectionStatus) -> Unit
// Вызывается, когда друг начинает или прекращает печатать сообщение
typealias FriendTypingHandler = (publicKey: String, isTyping: Boolean) -> Unit
// Вызывается, когда друг запрашивает у нас чанк отправляемого файла
typealias FileChunkRequestHandler = (publicKey: String, fileNo: Int, position: Long, length: Int) -> Unit

// Вызывается при получении приглашения в группу
typealias ConferenceInviteHandler = (friendNo: Int, type: Int, cookie: ByteArray) -> Unit
// Вызывается при получении нового сообщения в групповом чате
typealias ConferenceMessageHandler = (conferenceNo: Int, peerNo: Int, type: ToxMessageType, message: String) -> Unit
// Вызывается, когда кто-то входит или выходит из группы
typealias ConferencePeerListChangedHandler = (conferenceNo: Int) -> Unit
// Вызывается при изменении имени одного из участников группы
typealias ConferencePeerNameHandler = (conferenceNo: Int, peerNo: Int, newName: String) -> Unit
// Вызывается при изменении названия (заголовка) группы одним из участников
typealias ConferenceTitleHandler = (conferenceNo: Int, peerNo: Int, newTitle: String) -> Unit

// Вызывается при получении приглашения в NGC-группу
typealias GroupInviteHandler = (friendNo: Int, inviteData: ByteArray, groupName: String) -> Unit
// Вызывается при получении нового сообщения в NGC-группе
typealias GroupMessageHandler = (groupNo: Int, peerId: Int, type: ToxMessageType, message: String, messageId: Int) -> Unit
// Вызывается, когда участник входит в NGC-группу
typealias GroupPeerJoinHandler = (groupNo: Int, peerId: Int) -> Unit
// Вызывается, когда участник выходит из NGC-группы
typealias GroupPeerExitHandler = (groupNo: Int, peerId: Int, exitType: ToxGroupExitType) -> Unit
// Вызывается при изменении темы NGC-группы
typealias GroupTopicHandler = (groupNo: Int, peerId: Int, topic: String) -> Unit
// Вызывается при изменении имени одного из участников NGC-группы
typealias GroupPeerNameHandler = (groupNo: Int, peerId: Int, name: String) -> Unit
// Вызывается при изменении пароля NGC-группы
typealias GroupPasswordHandler = (groupNo: Int, password: ByteArray) -> Unit
// Вызывается при изменении статуса присутствия участника в NGC-группе
typealias GroupPeerStatusHandler = (groupNo: Int, peerId: Int, status: ToxUserStatus) -> Unit
// Вызывается при изменении типа приватности NGC-группы
typealias GroupPrivacyStateHandler = (groupNo: Int, privacyState: ToxGroupPrivacyState) -> Unit
// Вызывается при изменении голосового режима NGC-группы
typealias GroupVoiceStateHandler = (groupNo: Int, voiceState: ToxGroupVoiceState) -> Unit
// Вызывается при блокировке/разблокировке темы NGC-группы
typealias GroupTopicLockHandler = (groupNo: Int, topicLock: ToxGroupTopicLock) -> Unit
// Вызывается при изменении лимита участников в NGC-группе
typealias GroupPeerLimitHandler = (groupNo: Int, peerLimit: Int) -> Unit
// Вызывается при получении личного сообщения внутри NGC-группы
typealias GroupPrivateMessageHandler = (groupNo: Int, peerId: Int, type: ToxMessageType, message: String, messageId: Int) -> Unit
// Вызывается при успешном подключении клиента к NGC-группе
typealias GroupSelfJoinHandler = (groupNo: Int) -> Unit
// Вызывается при ошибке входа клиента в NGC-группу
typealias GroupJoinFailHandler = (groupNo: Int, failType: ToxGroupJoinFail) -> Unit
// Вызывается при совершении административных действий в NGC-группе
typealias GroupModerationHandler = (groupNo: Int, sourcePeerId: Int, targetPeerId: Int, modType: ToxGroupModEvent) -> Unit

class ToxEventListener @Inject constructor() {
    var contactMapping: List<Pair<PublicKey, Int>> = listOf()

    // Обработчики событий приватного чата и передачи файлов на стороне Kotlin

    // Получение lossless-пакета
    var friendLosslessPacketHandler: FriendLosslessPacketHandler = { _, _ -> }
    // Изменение контроля передачи файла
    var fileRecvControlHandler: FileRecvControlHandler = { _, _, _ -> }
    // Изменение статус-сообщения друга
    var friendStatusMessageHandler: FriendStatusMessageHandler = { _, _ -> }
    // Подтверждение прочтения сообщения
    var friendReadReceiptHandler: FriendReadReceiptHandler = { _, _ -> }
    // Изменение онлайн-статуса друга
    var friendStatusHandler: FriendStatusHandler = { _, _ -> }
    // Изменение сетевого статуса подключения друга
    var friendConnectionStatusHandler: FriendConnectionStatusHandler = { _, _ -> }
    // Получение запроса дружбы
    var friendRequestHandler: FriendRequestHandler = { _, _, _ -> }
    // Получение личного сообщения
    var friendMessageHandler: FriendMessageHandler = { _, _, _, _ -> }
    // Изменение имени друга
    var friendNameHandler: FriendNameHandler = { _, _ -> }
    // Получение чанка входящего файла
    var fileRecvChunkHandler: FileRecvChunkHandler = { _, _, _, _ -> }
    // Получение предложения файла
    var fileRecvHandler: FileRecvHandler = { _, _, _, _, _ -> }
    // Получение lossy-пакета
    var friendLossyPacketHandler: FriendLossyPacketHandler = { _, _ -> }
    // Изменение нашего сетевого статуса
    var selfConnectionStatusHandler: SelfConnectionStatusHandler = { _ -> }
    // Изменение статуса печати друга
    var friendTypingHandler: FriendTypingHandler = { _, _ -> }
    // Запрос чанка нашего файла со стороны друга
    var fileChunkRequestHandler: FileChunkRequestHandler = { _, _, _, _ -> }

    // Регистрация обработчиков конференций на Kotlin-стороне
    var conferenceInviteHandler: ConferenceInviteHandler = { _, _, _ -> }
    var conferenceMessageHandler: ConferenceMessageHandler = { _, _, _, _ -> }
    var conferencePeerListChangedHandler: ConferencePeerListChangedHandler = { _ -> }
    var conferencePeerNameHandler: ConferencePeerNameHandler = { _, _, _ -> }
    // Изменение названия группы
    var conferenceTitleHandler: ConferenceTitleHandler = { _, _, _ -> }

    // Регистрация обработчиков NGC-групп на Kotlin-стороне
    var groupInviteHandler: GroupInviteHandler = { _, _, _ -> }
    var groupMessageHandler: GroupMessageHandler = { _, _, _, _, _ -> }
    var groupPeerJoinHandler: GroupPeerJoinHandler = { _, _ -> }
    var groupPeerExitHandler: GroupPeerExitHandler = { _, _, _ -> }
    var groupTopicHandler: GroupTopicHandler = { _, _, _ -> }
    var groupPeerNameHandler: GroupPeerNameHandler = { _, _, _ -> }
    var groupPasswordHandler: GroupPasswordHandler = { _, _ -> }
    var groupPeerStatusHandler: GroupPeerStatusHandler = { _, _, _ -> }
    var groupPrivacyStateHandler: GroupPrivacyStateHandler = { _, _ -> }
    var groupVoiceStateHandler: GroupVoiceStateHandler = { _, _ -> }
    var groupTopicLockHandler: GroupTopicLockHandler = { _, _ -> }
    var groupPeerLimitHandler: GroupPeerLimitHandler = { _, _ -> }
    var groupPrivateMessageHandler: GroupPrivateMessageHandler = { _, _, _, _, _ -> }
    var groupSelfJoinHandler: GroupSelfJoinHandler = { _ -> }
    var groupJoinFailHandler: GroupJoinFailHandler = { _, _ -> }
    var groupModerationHandler: GroupModerationHandler = { _, _, _, _ -> }

    private fun keyFor(friendNo: Int) = contactMapping.find { it.second == friendNo }!!.first.string()

    fun friendLosslessPacket(friendNo: Int, data: ByteArray) =
        friendLosslessPacketHandler(keyFor(friendNo), data)

    fun fileRecvControl(friendNo: Int, fileNo: Int, control: ToxFileControl) =
        fileRecvControlHandler(keyFor(friendNo), fileNo, control)

    fun friendStatusMessage(friendNo: Int, message: ByteArray) =
        friendStatusMessageHandler(keyFor(friendNo), String(message))

    fun friendReadReceipt(friendNo: Int, messageId: Int) =
        friendReadReceiptHandler(keyFor(friendNo), messageId)

    fun friendStatus(friendNo: Int, status: ToxUserStatus) =
        friendStatusHandler(keyFor(friendNo), status.toUserStatus())

    fun friendConnectionStatus(friendNo: Int, status: ToxConnection) =
        friendConnectionStatusHandler(keyFor(friendNo), status.toConnectionStatus())

    fun friendRequest(publicKey: ByteArray, timeDelta: Int, message: ByteArray) =
        friendRequestHandler(publicKey.bytesToHex(), timeDelta, String(message))

    fun friendMessage(friendNo: Int, type: ToxMessageType, timeDelta: Int, message: ByteArray) =
        friendMessageHandler(keyFor(friendNo), type, timeDelta, String(message))

    fun friendName(friendNo: Int, newName: ByteArray) =
        friendNameHandler(keyFor(friendNo), String(newName))

    fun fileRecvChunk(friendNo: Int, fileNo: Int, position: Long, data: ByteArray) =
        fileRecvChunkHandler(keyFor(friendNo), fileNo, position, data)

    fun fileRecv(friendNo: Int, fileNo: Int, kind: Int, fileSize: Long, filename: ByteArray) =
        fileRecvHandler(keyFor(friendNo), fileNo, kind, fileSize, String(filename))

    fun friendLossyPacket(friendNo: Int, data: ByteArray) =
        friendLossyPacketHandler(keyFor(friendNo), data)

    fun selfConnectionStatus(connectionStatus: ToxConnection) =
        selfConnectionStatusHandler(connectionStatus.toConnectionStatus())

    fun friendTyping(friendNo: Int, isTyping: Boolean) =
        friendTypingHandler(keyFor(friendNo), isTyping)

    fun fileChunkRequest(friendNo: Int, fileNo: Int, position: Long, length: Int) =
        fileChunkRequestHandler(keyFor(friendNo), fileNo, position, length)

    // ===================================================================================
    // JNI Коллбеки (Вызываются напрямую из нативного C++ слоя NativeToxCore.cpp через JNI)
    // ===================================================================================

    /**
     * Вызывается из C++ коллбека `cb_friend_message`.
     * Пробрасывает новое приватное текстовое сообщение от друга.
     */
    fun onFriendMessage(friendNo: Int, type: Int, timeDelta: Int, message: ByteArray) =
        friendMessage(friendNo, ToxMessageType.fromInt(type), timeDelta, message)

    /**
     * Вызывается из C++ коллбека `cb_friend_request`.
     * Пробрасывает входящий запрос на добавление в друзья.
     */
    fun onFriendRequest(publicKey: ByteArray, timeDelta: Int, message: ByteArray) =
        friendRequest(publicKey, timeDelta, message)

    /**
     * Вызывается из C++ коллбека `cb_friend_connection_status`.
     * Уведомляет об изменении сетевого статуса друга (онлайн/офлайн).
     */
    fun onFriendConnectionStatus(friendNo: Int, status: Int) =
        friendConnectionStatus(friendNo, ToxConnection.fromInt(status))

    /**
     * Вызывается из C++ коллбека `cb_self_connection_status`.
     * Уведомляет об изменении нашего собственного сетевого статуса подключения к DHT.
     */
    fun onSelfConnectionStatus(status: Int) =
        selfConnectionStatus(ToxConnection.fromInt(status))

    /**
     * Вызывается из C++ коллбека `cb_friend_status`.
     * Уведомляет об изменении онлайн-статуса друга (в сети, отошел, не беспокоить).
     */
    fun onFriendStatus(friendNo: Int, status: Int) =
        friendStatus(friendNo, ToxUserStatus.fromInt(status))

    /**
     * Вызывается из C++ коллбека `cb_friend_status_message`.
     * Уведомляет об изменении статус-сообщения друга.
     */
    fun onFriendStatusMessage(friendNo: Int, message: ByteArray) =
        friendStatusMessage(friendNo, message)

    /**
     * Вызывается из C++ коллбека `cb_friend_name`.
     * Уведомляет об изменении имени друга.
     */
    fun onFriendName(friendNo: Int, name: ByteArray) =
        friendName(friendNo, name)

    /**
     * Вызывается из C++ коллбека `cb_friend_typing`.
     * Уведомляет о том, начал или прекратил друг писать сообщение.
     */
    fun onFriendTyping(friendNo: Int, isTyping: Boolean) =
        friendTyping(friendNo, isTyping)

    /**
     * Вызывается из C++ коллбека `cb_friend_read_receipt`.
     * Уведомляет о прочтении другом отправленного нами сообщения.
     */
    fun onFriendReadReceipt(friendNo: Int, messageId: Int) =
        friendReadReceipt(friendNo, messageId)

    /**
     * Вызывается из C++ коллбека `cb_file_recv`.
     * Уведомляет о том, что друг предлагает нам принять файл.
     */
    fun onFileRecv(friendNo: Int, fileNo: Int, kind: Int, fileSize: Long, filename: ByteArray) =
        fileRecv(friendNo, fileNo, kind, fileSize, filename)

    /**
     * Вызывается из C++ коллбека `cb_file_recv_control`.
     * Уведомляет об изменении контроля передачи файла (пауза, возобновление, отмена).
     */
    fun onFileRecvControl(friendNo: Int, fileNo: Int, control: Int) =
        fileRecvControl(friendNo, fileNo, ToxFileControl.fromInt(control))

    /**
     * Вызывается из C++ коллбека `cb_file_recv_chunk`.
     * Уведомляет о получении очередного байтового чанка (куска) входящего файла.
     */
    fun onFileRecvChunk(friendNo: Int, fileNo: Int, position: Long, data: ByteArray) =
        fileRecvChunk(friendNo, fileNo, position, data)

    /**
     * Вызывается из C++ коллбека `cb_file_chunk_request`.
     * Уведомляет о том, что получатель запрашивает у нас следующий чанк отправляемого файла.
     */
    fun onFileChunkRequest(friendNo: Int, fileNo: Int, position: Long, length: Int) =
        fileChunkRequest(friendNo, fileNo, position, length)

    /**
     * Вызывается из C++ коллбека `cb_friend_lossless_packet`.
     * Уведомляет о получении служебного lossless-пакета от друга.
     */
    fun onFriendLosslessPacket(friendNo: Int, data: ByteArray) =
        friendLosslessPacket(friendNo, data)

    /**
     * Вызывается из C++ коллбека `cb_friend_lossy_packet`.
     * Уведомляет о получении ненадежного lossy-пакета от друга.
     */
    fun onFriendLossyPacket(friendNo: Int, data: ByteArray) =
        friendLossyPacket(friendNo, data)

    // JNI коллбеки для групповых чатов (конференций)
    fun onConferenceInvite(friendNo: Int, type: Int, cookie: ByteArray) =
        conferenceInviteHandler(friendNo, type, cookie)

    fun onConferenceMessage(conferenceNo: Int, peerNo: Int, type: Int, message: ByteArray) =
        conferenceMessageHandler(conferenceNo, peerNo, ToxMessageType.fromInt(type), String(message))

    fun onConferencePeerListChanged(conferenceNo: Int) =
        conferencePeerListChangedHandler(conferenceNo)

    fun onConferencePeerName(conferenceNo: Int, peerNo: Int, name: ByteArray) =
        conferencePeerNameHandler(conferenceNo, peerNo, String(name))

    fun onConferenceTitle(conferenceNo: Int, peerNo: Int, title: ByteArray) =
        conferenceTitleHandler(conferenceNo, peerNo, String(title))

    // JNI коллбеки для NGC-групп (вызываются нативно)
    fun onGroupInvite(friendNo: Int, inviteData: ByteArray, groupName: ByteArray) =
        groupInviteHandler(friendNo, inviteData, String(groupName))

    fun onGroupMessage(groupNo: Int, peerId: Int, type: Int, message: ByteArray, messageId: Int) =
        groupMessageHandler(groupNo, peerId, ToxMessageType.fromInt(type), String(message), messageId)

    fun onGroupPeerJoin(groupNo: Int, peerId: Int) =
        groupPeerJoinHandler(groupNo, peerId)

    fun onGroupPeerExit(groupNo: Int, peerId: Int, exitType: Int) =
        groupPeerExitHandler(groupNo, peerId, ToxGroupExitType.fromInt(exitType))

    fun onGroupTopic(groupNo: Int, peerId: Int, topic: ByteArray) =
        groupTopicHandler(groupNo, peerId, String(topic))

    fun onGroupPeerName(groupNo: Int, peerId: Int, name: ByteArray) =
        groupPeerNameHandler(groupNo, peerId, String(name))

    fun onGroupPassword(groupNo: Int, password: ByteArray) =
        groupPasswordHandler(groupNo, password)

    fun onGroupPeerStatus(groupNo: Int, peerId: Int, status: Int) =
        groupPeerStatusHandler(groupNo, peerId, ToxUserStatus.fromInt(status))

    fun onGroupPrivacyState(groupNo: Int, privacyState: Int) =
        groupPrivacyStateHandler(groupNo, ToxGroupPrivacyState.fromInt(privacyState))

    fun onGroupVoiceState(groupNo: Int, voiceState: Int) =
        groupVoiceStateHandler(groupNo, ToxGroupVoiceState.fromInt(voiceState))

    fun onGroupTopicLock(groupNo: Int, topicLock: Int) =
        groupTopicLockHandler(groupNo, ToxGroupTopicLock.fromInt(topicLock))

    fun onGroupPeerLimit(groupNo: Int, peerLimit: Int) =
        groupPeerLimitHandler(groupNo, peerLimit)

    fun onGroupPrivateMessage(groupNo: Int, peerId: Int, type: Int, message: ByteArray, messageId: Int) =
        groupPrivateMessageHandler(groupNo, peerId, ToxMessageType.fromInt(type), String(message), messageId)

    fun onGroupSelfJoin(groupNo: Int) =
        groupSelfJoinHandler(groupNo)

    fun onGroupJoinFail(groupNo: Int, failType: Int) =
        groupJoinFailHandler(groupNo, ToxGroupJoinFail.fromInt(failType))

    fun onGroupModeration(groupNo: Int, sourcePeerId: Int, targetPeerId: Int, modType: Int) =
        groupModerationHandler(groupNo, sourcePeerId, targetPeerId, ToxGroupModEvent.fromInt(modType))
}
