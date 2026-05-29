// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.infrastructure.tox

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.IToxGroupManager
import ltd.evilcorp.domain.core.network.IToxProfile
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxGroupRole
import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.transfer.model.FileKind
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.GroupManager
import ltd.evilcorp.domain.features.group.GroupDataRepositories
import ltd.evilcorp.domain.features.group.GroupSessionCoordinator
import ltd.evilcorp.domain.features.group.IGroupConnectionScheduler
import ltd.evilcorp.domain.features.group.IGroupSessionRegistry
import ltd.evilcorp.domain.features.group.GroupInvite
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class GroupSyncManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = CoroutineScope(testDispatcher)

    private val fakeGroupRepository = FakeGroupRepository()
    private val fakeContactRepository = FakeContactRepository()
    private val fakeMessageRepository = FakeMessageRepository()
    private val fakeTox = FakeTox()
    private val fakeConnectionScheduler = FakeConnectionScheduler()
    private val fakeSessionRegistry = FakeGroupSessionRegistry()

    private lateinit var groupManager: GroupManager
    private lateinit var groupSyncManager: GroupSyncManager

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val groupDataRepositories = GroupDataRepositories(
            group = fakeGroupRepository,
            contact = fakeContactRepository,
            message = fakeMessageRepository
        )
        val groupSessionCoordinator = GroupSessionCoordinator(
            connectionScheduler = fakeConnectionScheduler,
            sessionRegistry = fakeSessionRegistry
        )
        val groupToxServices = ltd.evilcorp.domain.features.group.GroupToxServices(
            tox = fakeTox,
            profile = fakeTox
        )
        val groupConnectionService = ltd.evilcorp.domain.features.group.GroupConnectionService(
            scope = testScope,
            sessionCoordinator = groupSessionCoordinator,
            groupRepository = fakeGroupRepository,
            toxServices = groupToxServices
        )
        val groupMessagingService = ltd.evilcorp.domain.features.group.GroupMessagingService(
            scope = testScope,
            groupRepository = fakeGroupRepository,
            messageRepository = fakeMessageRepository,
            contactRepository = fakeContactRepository,
            toxServices = groupToxServices,
            sessionCoordinator = groupSessionCoordinator
        )
        val groupServices = ltd.evilcorp.domain.features.group.GroupServices(
            connection = groupConnectionService,
            messaging = groupMessagingService
        )
        groupManager = GroupManager(
            scope = testScope,
            repositories = groupDataRepositories,
            chatManager = ChatManager(testScope, fakeContactRepository, fakeMessageRepository, fakeTox),
            toxServices = groupToxServices,
            sessionCoordinator = groupSessionCoordinator,
            services = groupServices
        )

        groupSyncManager = GroupSyncManager(
            scope = testScope,
            groupRepository = fakeGroupRepository,
            tox = fakeTox,
            groupManager = groupManager
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testOnGroupPeerOnline_reconnectsAndSendsSummary() = runTest(testDispatcher) {
        val chatId = "test_chat_id"
        val peerPk = "ABCD1234ABCD1234ABCD1234ABCD1234ABCD1234ABCD1234ABCD1234ABCD1234"
        
        // Setup a disconnected group with peer present
        val group = Group(
            chatId = chatId,
            name = "Test Group",
            connected = false,
            groupNumber = 42
        )
        fakeGroupRepository.groups[chatId] = group
        fakeGroupRepository.peers[chatId] = listOf(
            GroupPeer(groupChatId = chatId, peerId = 0, publicKey = "OUR_PK", isOurselves = true),
            GroupPeer(groupChatId = chatId, peerId = 1, publicKey = peerPk, isOurselves = false)
        )
        fakeGroupRepository.messageIds[chatId] = listOf(10, 11, 12)
        
        groupManager.setConnectionStatus(chatId, GroupConnectionStatus.Disconnected)

        groupSyncManager.onGroupPeerOnline(peerPk)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify reconnect attempt
        assertTrue(fakeTox.reconnectedGroups.contains(42))

        // Verify summary packet was sent
        assertEquals(1, fakeTox.sentPackets.size)
        val packet = fakeTox.sentPackets.first()
        assertEquals(peerPk, packet.first.string())
        
        val payload = packet.second.copyOfRange(1, packet.second.size).decodeToString()
        val json = JSONObject(payload)
        assertEquals("group_sync_summary", json.getString("type"))
        assertEquals(chatId, json.getString("chatId"))
        assertEquals(3, json.getInt("count"))
        assertEquals(12, json.getInt("lastId"))
    }

    @Test
    fun testOnGroupPeerOnline_bootstrapOnly_reconnectsButNoSummary() = runTest(testDispatcher) {
        val chatId = "test_chat_id"
        val peerPk = "BOOTSTRAP_PEER"
        
        val group = Group(
            chatId = chatId,
            name = "Test Group",
            connected = false,
            groupNumber = 42
        )
        fakeGroupRepository.groups[chatId] = group
        fakeGroupRepository.peers[chatId] = listOf(
            GroupPeer(groupChatId = chatId, peerId = 0, publicKey = "OUR_PK", isOurselves = true),
            GroupPeer(groupChatId = chatId, peerId = 1, publicKey = peerPk, isOurselves = false)
        )
        
        fakeConnectionScheduler.bootstrapFriends.add(peerPk)
        groupManager.setConnectionStatus(chatId, GroupConnectionStatus.Disconnected)

        groupSyncManager.onGroupPeerOnline(peerPk)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should trigger reconnect
        assertTrue(fakeTox.reconnectedGroups.contains(42))
        // Should NOT send summary to bootstrap-only friend
        assertTrue(fakeTox.sentPackets.isEmpty())
    }

    @Test
    fun testHandleLosslessPacket_summaryMatch_inSyncNoReply() = runTest(testDispatcher) {
        val chatId = "test_chat"
        val peerPk = "PEER_PK"
        
        fakeGroupRepository.messageIds[chatId] = listOf(1, 2, 3)

        val json = JSONObject()
        json.put("type", "group_sync_summary")
        json.put("chatId", chatId)
        json.put("count", 3)
        json.put("lastId", 3)

        val packetData = byteArrayOf(0xA0.toByte()) + json.toString().toByteArray()

        groupSyncManager.handleLosslessPacket(peerPk, packetData)
        testDispatcher.scheduler.advanceUntilIdle()

        // Match, so no reply packets sent
        assertTrue(fakeTox.sentPackets.isEmpty())
    }

    @Test
    fun testHandleLosslessPacket_summaryMismatch_sendsIdsPage() = runTest(testDispatcher) {
        val chatId = "test_chat"
        val peerPk = "PEER_PK"
        
        fakeGroupRepository.messageIds[chatId] = listOf(1, 2, 3, 4, 5)

        val json = JSONObject()
        json.put("type", "group_sync_summary")
        json.put("chatId", chatId)
        json.put("count", 3) // Peer has fewer messages
        json.put("lastId", 3)

        val packetData = byteArrayOf(0xA0.toByte()) + json.toString().toByteArray()

        groupSyncManager.handleLosslessPacket(peerPk, packetData)
        testDispatcher.scheduler.advanceUntilIdle()

        // Mismatch, should send IDS_PAGE
        assertEquals(1, fakeTox.sentPackets.size)
        val packet = fakeTox.sentPackets.first()
        assertEquals(peerPk, packet.first.string())

        val payload = packet.second.copyOfRange(1, packet.second.size).decodeToString()
        val res = JSONObject(payload)
        assertEquals("group_sync_ids_page", res.getString("type"))
        assertEquals(chatId, res.getString("chatId"))
        assertFalse(res.getBoolean("more"))
        
        val idsJson = res.getJSONArray("ids")
        assertEquals(5, idsJson.length())
        assertEquals(1, idsJson.getInt(0))
        assertEquals(5, idsJson.getInt(4))
    }

    @Test
    fun testHandleLosslessPacket_idsPageAccumulationAndComparison() = runTest(testDispatcher) {
        val chatId = "test_chat"
        val peerPk = "PEER_PK"

        // Local messages: 1, 2, 3, 4
        fakeGroupRepository.messageIds[chatId] = listOf(1, 2, 3, 4)
        fakeGroupRepository.messages[chatId] = mutableListOf(
            createMessage(chatId, 3, "Msg 3"),
            createMessage(chatId, 4, "Msg 4")
        )

        // Send first page (more = true)
        val page1 = JSONObject()
        page1.put("type", "group_sync_ids_page")
        page1.put("chatId", chatId)
        page1.put("page", 0)
        page1.put("more", true)
        val arr1 = JSONArray()
        arr1.put(1)
        page1.put("ids", arr1)

        val packet1 = byteArrayOf(0xA0.toByte()) + page1.toString().toByteArray()
        groupSyncManager.handleLosslessPacket(peerPk, packet1)
        testDispatcher.scheduler.advanceUntilIdle()

        // No packets sent yet, still accumulating
        assertTrue(fakeTox.sentPackets.isEmpty())

        // Send second page (more = false) - Peer has IDs 1 and 2
        val page2 = JSONObject()
        page2.put("type", "group_sync_ids_page")
        page2.put("chatId", chatId)
        page2.put("page", 1)
        page2.put("more", false)
        val arr2 = JSONArray()
        arr2.put(2)
        page2.put("ids", arr2)

        val packet2 = byteArrayOf(0xA0.toByte()) + page2.toString().toByteArray()
        groupSyncManager.handleLosslessPacket(peerPk, packet2)
        testDispatcher.scheduler.advanceUntilIdle()

        // Accumulation done. We have 1,2,3,4. Peer has 1,2. We must send 3,4.
        assertEquals(1, fakeTox.sentPackets.size)
        val reply = fakeTox.sentPackets.first()
        val payload = reply.second.copyOfRange(1, reply.second.size).decodeToString()
        val res = JSONObject(payload)
        assertEquals("group_sync_msg_page", res.getString("type"))
        assertEquals(chatId, res.getString("chatId"))
        
        val msgsJson = res.getJSONArray("messages")
        assertEquals(2, msgsJson.length())
        assertEquals("Msg 3", msgsJson.getJSONObject(0).getString("message"))
        assertEquals("Msg 4", msgsJson.getJSONObject(1).getString("message"))
    }

    @Test
    fun testHandleLosslessPacket_msgPageSavesMessages() = runTest(testDispatcher) {
        val chatId = "test_chat"
        val peerPk = "PEER_PK"

        // Local messages: 1 (2 is missing)
        fakeGroupRepository.messageIds[chatId] = listOf(1)
        fakeGroupRepository.existsResult[1] = true
        fakeGroupRepository.existsResult[2] = false

        val msgObj = JSONObject()
        msgObj.put("peerId", 1)
        msgObj.put("senderName", "Alice")
        msgObj.put("message", "Hello")
        msgObj.put("timestamp", 123456789L)
        msgObj.put("correlationId", 2)
        msgObj.put("type", "Normal")

        val json = JSONObject()
        json.put("type", "group_sync_msg_page")
        json.put("chatId", chatId)
        json.put("more", false)
        val arr = JSONArray()
        arr.put(msgObj)
        json.put("messages", arr)

        val packet = byteArrayOf(0xA0.toByte()) + json.toString().toByteArray()
        groupSyncManager.handleLosslessPacket(peerPk, packet)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify message was saved
        assertEquals(1, fakeGroupRepository.addedMessages.size)
        val saved = fakeGroupRepository.addedMessages.first()
        assertEquals(chatId, saved.groupChatId)
        assertEquals(2, saved.correlationId)
        assertEquals("Hello", saved.message)
        assertEquals(123456789L, saved.timestamp)
    }

    private fun createMessage(chatId: String, corrId: Int, body: String): GroupMessage {
        return GroupMessage(
            groupChatId = chatId,
            peerId = 0,
            senderName = "Me",
            message = body,
            sender = Sender.Sent,
            type = MessageType.Normal,
            correlationId = corrId,
            timestamp = 1000L
        )
    }

    // --- Stub / Fake Implementations ---

    private class FakeGroupRepository : IGroupRepository {
        val groups = ConcurrentHashMap<String, Group>()
        val peers = ConcurrentHashMap<String, List<GroupPeer>>()
        val messageIds = ConcurrentHashMap<String, List<Int>>()
        val messages = ConcurrentHashMap<String, MutableList<GroupMessage>>()
        val addedMessages = mutableListOf<GroupMessage>()
        val existsResult = mutableMapOf<Int, Boolean>()

        override fun get(chatId: String): Flow<Group?> = flowOf(groups[chatId])
        override suspend fun getDirect(chatId: String): Group? = groups[chatId]
        override fun getAll(): Flow<List<Group>> = flowOf(groups.values.toList())
        override suspend fun exists(chatId: String): Boolean = groups.containsKey(chatId)
        override suspend fun add(group: Group) { groups[group.chatId] = group }
        override suspend fun update(group: Group) { groups[group.chatId] = group }
        override suspend fun delete(group: Group) { groups.remove(group.chatId) }
        override suspend fun deleteByChatId(chatId: String) { groups.remove(chatId) }
        override suspend fun setName(chatId: String, name: String) {}
        override suspend fun setTopic(chatId: String, topic: String) {}
        override suspend fun setPasswordProtected(chatId: String, protected: Boolean) {}
        override suspend fun setPrivacyState(chatId: String, privacyState: GroupPrivacyState) {}
        override suspend fun setPeerCount(chatId: String, peerCount: Int) {}
        override suspend fun setSelfPeerId(chatId: String, peerId: Int) {}
        override suspend fun setSelfRole(chatId: String, role: String) {}
        override suspend fun setLastMessage(chatId: String, lastMessage: Long) {}
        override suspend fun setHasUnreadMessages(chatId: String, hasUnread: Boolean) {}
        override suspend fun setDraftMessage(chatId: String, draft: String) {}
        override suspend fun setConnected(chatId: String, connected: Boolean) {}
        override suspend fun setGroupNumber(chatId: String, groupNumber: Int) {}
        override suspend fun findChatIdByGroupNumber(groupNumber: Int): String? = null
        override suspend fun addMessage(message: GroupMessage) { addedMessages.add(message) }
        override fun getMessages(groupChatId: String): Flow<List<GroupMessage>> = flowOf(messages[groupChatId] ?: emptyList())
        override suspend fun getPendingMessages(groupChatId: String): List<GroupMessage> = emptyList()
        override suspend fun getUnsentMessages(groupChatId: String): List<GroupMessage> = emptyList()
        override suspend fun setCorrelationId(id: Long, correlationId: Int) {}
        override suspend fun deleteMessages(groupChatId: String) {}
        override suspend fun deleteMessage(id: Long) {}
        override suspend fun setReceipt(groupChatId: String, correlationId: Int, timestamp: Long) {}
        override suspend fun existsByCorrelationId(groupChatId: String, correlationId: Int): Boolean = existsResult[correlationId] ?: false
        override suspend fun getMessageIds(groupChatId: String): List<Int> = messageIds[groupChatId] ?: emptyList()
        override suspend fun getMessagesByIds(
            groupChatId: String,
            ids: Set<Int>
        ): List<GroupMessage> = messages[groupChatId]?.filter { ids.contains(it.correlationId) } ?: emptyList()
        override suspend fun addPeer(peer: GroupPeer) {}
        override suspend fun updatePeer(peer: GroupPeer) {}
        override suspend fun deletePeer(peer: GroupPeer) {}
        override suspend fun deletePeerById(groupChatId: String, peerId: Int) {}
        override suspend fun deleteAllPeers(groupChatId: String) {}
        override fun getPeers(groupChatId: String): Flow<List<GroupPeer>> = flowOf(peers[groupChatId] ?: emptyList())
        override fun getPeer(groupChatId: String, peerId: Int): Flow<GroupPeer?> = flowOf(null)
        override suspend fun getPeerNameDirect(groupChatId: String, peerId: Int): String? = null
        override suspend fun peerExistsDirect(groupChatId: String, peerId: Int): Boolean = false
        override suspend fun peerExistsByPublicKey(groupChatId: String, publicKey: String): Boolean = false
        override suspend fun deletePeerByPublicKey(groupChatId: String, publicKey: String) {}
        override suspend fun setPeerName(groupChatId: String, peerId: Int, name: String) {}
        override suspend fun setPeerRole(groupChatId: String, peerId: Int, role: String) {}
        override suspend fun setPeerStatus(groupChatId: String, peerId: Int, status: UserStatus) {}
        override fun peerCount(groupChatId: String): Flow<Int> = flowOf(0)
        override suspend fun peerCountDirect(groupChatId: String): Int = 0
    }

    private class FakeContactRepository : IContactRepository {
        override suspend fun exists(publicKey: String): Boolean = false
        override suspend fun add(contact: Contact) {}
        override suspend fun update(contact: Contact) {}
        override suspend fun delete(contact: Contact) {}
        override fun get(publicKey: String): Flow<Contact?> = flowOf(null)
        override fun getAll(): Flow<List<Contact>> = flowOf(emptyList())
        override suspend fun resetTransientData() {}
        override suspend fun setName(publicKey: String, name: String) {}
        override suspend fun setStatusMessage(publicKey: String, statusMessage: String) {}
        override suspend fun setLastMessage(publicKey: String, lastMessage: Long) {}
        override suspend fun setUserStatus(publicKey: String, status: UserStatus) {}
        override suspend fun setConnectionStatus(publicKey: String, status: ConnectionStatus) {}
        override suspend fun setTyping(publicKey: String, typing: Boolean) {}
        override suspend fun setAvatarUri(publicKey: String, uri: String) {}
        override suspend fun setHasUnreadMessages(publicKey: String, anyUnread: Boolean) {}
        override suspend fun setDraftMessage(publicKey: String, draft: String) {}
        override suspend fun setLastOnline(publicKey: String, lastOnline: Long) {}
    }

    private class FakeMessageRepository : IMessageRepository {
        override suspend fun add(message: Message) {}
        override fun get(conversation: String): Flow<List<Message>> = flowOf(emptyList())
        override suspend fun getPending(conversation: String): List<Message> = emptyList()
        override suspend fun setCorrelationId(id: Long, correlationId: Int) {}
        override suspend fun delete(conversation: String) {}
        override suspend fun deleteMessage(id: Long) {}
        override suspend fun setReceipt(conversation: String, correlationId: Int, timestamp: Long) {}
        override suspend fun exists(conversation: String, message: String): Boolean = false
    }

    private class FakeConnectionScheduler : IGroupConnectionScheduler {
        val bootstrapFriends = mutableSetOf<String>()
        override fun reconnectAll() {}
        override fun scheduleAutoReconnect(chatId: String, groupNumber: Int) {}
        override fun cancelReconnect(chatId: String) {}
        override fun stopReconnect(chatId: String) {}
        override fun isBootstrapFriend(pk: String): Boolean = bootstrapFriends.contains(pk)
    }

    private class FakeTox : ITox {
        override val toxId: ToxID get() = throw UnsupportedOperationException()
        override val publicKey: PublicKey = PublicKey("OUR_PK")
        override var nospam: Int get() = 0; set(_) {}
        override var started: Boolean = true
        override var isBootstrapNeeded: Boolean get() = false; set(_) {}
        override val password: String? get() = null

        val sentPackets = mutableListOf<Pair<PublicKey, ByteArray>>()
        val reconnectedGroups = mutableListOf<Int>()

        override fun changePassword(new: String?) {}
        override fun stop() {}
        override fun getContacts(): List<Pair<PublicKey, Int>> = emptyList()
        override fun acceptFriendRequest(publicKey: PublicKey) {}
        override fun startFileTransfer(pk: PublicKey, fileNumber: Int) {}
        override fun stopFileTransfer(pk: PublicKey, fileNumber: Int) {}
        override fun sendFile(
            pk: PublicKey,
            fileKind: ltd.evilcorp.domain.features.transfer.model.FileKind,
            fileSize: Long,
            fileName: String
        ): Int = 0
        override fun sendFileChunk(
            pk: PublicKey,
            fileNo: Int,
            pos: Long,
            data: ByteArray
        ): Result<Unit> = Result.success(Unit)
        override fun getName(): String = "Me"
        override fun setName(name: String) {}
        override fun getStatusMessage(): String = ""
        override fun setStatusMessage(statusMessage: String) {}
        override fun addContact(toxId: ToxID, message: String) {}
        override fun deleteContact(publicKey: PublicKey) {}
        override fun sendMessage(publicKey: PublicKey, message: String, type: MessageType): Int = 0
        override fun getSaveData(): ByteArray = byteArrayOf()
        override fun setTyping(publicKey: PublicKey, typing: Boolean): Boolean = true
        override fun friendGetTyping(publicKey: PublicKey): Boolean = false
        override fun getFriendNumber(publicKey: PublicKey): Int = 0
        override fun getFriendPublicKey(friendNumber: Int): PublicKey? = null
        override fun friendGetLastOnline(publicKey: PublicKey): Long = 0
        override fun getStatus(): UserStatus = UserStatus.None
        override fun setStatus(status: UserStatus) {}
        
        override fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): Boolean {
            sentPackets.add(Pair(pk, packet))
            return true
        }

        override fun startCall(pk: PublicKey): Boolean = true
        override fun answerCall(pk: PublicKey): Boolean = true
        override fun endCall(pk: PublicKey): Boolean = true
        override fun sendAudio(pk: PublicKey, pcm: ShortArray, channels: Int, samplingRate: Int): Boolean = true
        override fun groupNew(privacyState: ToxGroupPrivacyState, groupName: ByteArray, selfName: ByteArray): Int = 0
        override fun groupJoin(friendNo: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int = 0
        override fun groupLeave(groupNumber: Int): Boolean = true
        override fun groupSendMessage(groupNumber: Int, type: ToxMessageType, message: ByteArray): Int = 0
        override fun groupSetTopic(groupNumber: Int, topic: ByteArray): Boolean = true
        override fun groupGetTopic(groupNumber: Int): ByteArray? = null
        override fun groupGetName(groupNumber: Int): ByteArray? = null
        override fun groupGetChatId(groupNumber: Int): ByteArray? = null
        override fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean = true
        override fun groupGetPassword(groupNumber: Int): ByteArray? = null
        override fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? = null
        override fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? = null
        override fun groupSelfGetPeerId(groupNumber: Int): Int = 0
        override fun groupSelfGetRole(groupNumber: Int): ToxGroupRole = ToxGroupRole.USER
        override fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean = true
        override fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int = 0
        override fun groupReconnect(groupNumber: Int): Boolean {
            reconnectedGroups.add(groupNumber)
            return true
        }
        override fun addFriendNoRequest(publicKey: PublicKey): Int = 0
        override fun groupGetChatlist(): IntArray = intArrayOf()
    }

    private class FakeGroupSessionRegistry : IGroupSessionRegistry {
        override var activeGroup: String = ""
        private val _pendingInvite = MutableStateFlow<GroupInvite?>(null)
        override val pendingInvite = _pendingInvite
        private val _connectionStatuses = MutableStateFlow<Map<String, GroupConnectionStatus>>(emptyMap())
        override val connectionStatuses = _connectionStatuses

        override fun setPendingInvite(invite: GroupInvite?) {
            _pendingInvite.value = invite
        }

        override fun setConnectionStatus(chatId: String, status: GroupConnectionStatus) {
            _connectionStatuses.value = _connectionStatuses.value + (chatId to status)
        }

        override fun removeConnectionStatus(chatId: String) {
            _connectionStatuses.value = _connectionStatuses.value - chatId
        }
    }
}
