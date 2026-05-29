// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.infrastructure.tox

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import ltd.evilcorp.core.tox.GroupConnectionSchedulerImpl
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.IToxGroupManager
import ltd.evilcorp.domain.core.network.enums.ToxGroupPrivacyState
import ltd.evilcorp.domain.core.network.enums.ToxGroupRole
import ltd.evilcorp.domain.core.network.enums.ToxMessageType
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.GroupManager
import ltd.evilcorp.domain.features.group.GroupDataRepositories
import ltd.evilcorp.domain.features.group.GroupSessionCoordinator
import ltd.evilcorp.domain.features.group.IGroupSessionRegistry
import ltd.evilcorp.domain.features.group.GroupInvite
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import ltd.evilcorp.domain.core.network.hexToBytes
import ltd.evilcorp.domain.features.group.model.GroupMessage
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Provider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class GroupConnectionSchedulerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = CoroutineScope(testDispatcher)

    private val fakeGroupRepository = FakeGroupRepository()
    private val fakeContactRepository = FakeContactRepository()
    private val fakeTox = FakeTox()
    private val fakeSessionRegistry = FakeGroupSessionRegistry()

    private lateinit var groupManager: GroupManager
    private lateinit var scheduler: GroupConnectionSchedulerImpl

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val groupDataRepositories = GroupDataRepositories(
            group = fakeGroupRepository,
            contact = fakeContactRepository,
            message = FakeMessageRepository()
        )
        val groupSessionCoordinator = GroupSessionCoordinator(
            connectionScheduler = FakeConnectionScheduler(),
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
            messageRepository = FakeMessageRepository(),
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
            chatManager = ltd.evilcorp.domain.features.chat.ChatManager(testScope, fakeContactRepository, FakeMessageRepository(), fakeTox),
            toxServices = groupToxServices,
            sessionCoordinator = groupSessionCoordinator,
            services = groupServices
        )

        scheduler = GroupConnectionSchedulerImpl(
            scope = testScope,
            tox = fakeTox,
            groupRepository = fakeGroupRepository,
            contactRepository = fakeContactRepository,
            groupManagerProvider = Provider { groupManager }
        )
    }

    @AfterTest
    fun tearDown() {
        kotlinx.coroutines.Job(testScope.coroutineContext[kotlinx.coroutines.Job]).cancelChildren()
        Dispatchers.resetMain()
    }

    @Test
    fun testReconnectAll_whenGroupIsDisconnected_rejoinsDirectlyAndAddsBootstrapFriends() = runTest(testDispatcher) {
        val chatId = "1111111111111111111111111111111111111111111111111111111111111111"
        val group = Group(
            chatId = chatId,
            name = "Restoration Test Group",
            connected = false,
            groupNumber = -1
        )
        fakeGroupRepository.add(group)

        val pkA = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        val pkB = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"
        val pkC = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC"
        val pkD = "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD"

        // 4 peers in the group
        val peerA = GroupPeer(
            groupChatId = chatId,
            peerId = 1,
            name = "Peer A",
            publicKey = pkA
        )
        val peerB = GroupPeer(
            groupChatId = chatId,
            peerId = 2,
            name = "Peer B",
            publicKey = pkB
        )
        val peerC = GroupPeer(
            groupChatId = chatId,
            peerId = 3,
            name = "Peer C",
            publicKey = pkC
        )
        val peerD = GroupPeer(
            groupChatId = chatId,
            peerId = 4,
            name = "Peer D",
            publicKey = pkD,
            isOurselves = true
        )
        fakeGroupRepository.peers[chatId] = listOf(peerA, peerB, peerC, peerD)

        // Initially native C layer has no group
        fakeTox.chatList = intArrayOf()

        // Trigger reconnect
        scheduler.reconnectAll()
        testDispatcher.scheduler.runCurrent()

        // 1. Should call groupJoinDirect because groupNumber is -1 (or not active in C core)
        assertTrue(fakeTox.joinDirectCalledWith.any { it.first.contentEquals(chatId.hexToBytes()) })
        assertEquals(101, fakeGroupRepository.groups[chatId]?.groupNumber)

        // 2. Should add 3 other peers as temporary bootstrap friends
        assertTrue(fakeTox.bootstrapFriendsAdded.contains(PublicKey(pkA)))
        assertTrue(fakeTox.bootstrapFriendsAdded.contains(PublicKey(pkB)))
        assertTrue(fakeTox.bootstrapFriendsAdded.contains(PublicKey(pkC)))
        assertFalse(fakeTox.bootstrapFriendsAdded.contains(PublicKey(pkD))) // should not add ourselves

        // 3. Reconnect loop should trigger reconnection calls
        assertTrue(fakeTox.reconnectedGroups.contains(101))
        assertEquals(GroupConnectionStatus.Connecting, groupManager.connectionStatus(chatId))

        // 4. Cancel reconnect should clean up bootstrap friends
        scheduler.stopReconnect(chatId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeTox.contactsDeleted.contains(PublicKey(pkA)))
        assertTrue(fakeTox.contactsDeleted.contains(PublicKey(pkB)))
        assertTrue(fakeTox.contactsDeleted.contains(PublicKey(pkC)))
    }

    @Test
    fun testReconnectAll_whenGroupAlreadyActiveInNativeCore_startsReconnectLoopDirectly() = runTest(testDispatcher) {
        val chatId = "2222222222222222222222222222222222222222222222222222222222222222"
        val group = Group(
            chatId = chatId,
            name = "Active Group",
            connected = false,
            groupNumber = 42
        )
        fakeGroupRepository.add(group)

        // Peer online setup
        val peer = GroupPeer(groupChatId = chatId, peerId = 1, name = "Peer A", publicKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
        fakeGroupRepository.peers[chatId] = listOf(peer)

        // Setup active C core group
        fakeTox.chatList = intArrayOf(42)
        fakeTox.chatIdMock[42] = chatId.hexToBytes()

        // Trigger reconnect
        scheduler.reconnectAll()
        testDispatcher.scheduler.runCurrent()

        // Should NOT call joinDirect since it is active in core
        assertTrue(fakeTox.joinDirectCalledWith.isEmpty())

        // Reconnect loop should start on native groupNumber 42
        assertTrue(fakeTox.reconnectedGroups.contains(42))
        assertEquals(GroupConnectionStatus.Connecting, groupManager.connectionStatus(chatId))

        // Clean up
        scheduler.stopReconnect(chatId)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    // --- Fakes ---

    private class FakeGroupRepository : IGroupRepository {
        val groups = ConcurrentHashMap<String, Group>()
        val peers = ConcurrentHashMap<String, List<GroupPeer>>()

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
        override suspend fun setConnected(chatId: String, connected: Boolean) {
            groups[chatId]?.let { it.connected = connected }
        }
        override suspend fun setGroupNumber(chatId: String, groupNumber: Int) {
            groups[chatId]?.let { groups[chatId] = it.copy(groupNumber = groupNumber) }
        }
        override suspend fun findChatIdByGroupNumber(groupNumber: Int): String? = groups.values.find { it.groupNumber == groupNumber }?.chatId
        override suspend fun addMessage(message: GroupMessage) {}
        override fun getMessages(groupChatId: String): Flow<List<GroupMessage>> = flowOf(emptyList())
        override suspend fun getPendingMessages(groupChatId: String): List<GroupMessage> = emptyList()
        override suspend fun getUnsentMessages(groupChatId: String): List<GroupMessage> = emptyList()
        override suspend fun setCorrelationId(id: Long, correlationId: Int) {}
        override suspend fun deleteMessages(groupChatId: String) {}
        override suspend fun deleteMessage(id: Long) {}
        override suspend fun setReceipt(groupChatId: String, correlationId: Int, timestamp: Long) {}
        override suspend fun existsByCorrelationId(groupChatId: String, correlationId: Int): Boolean = false
        override suspend fun getMessageIds(groupChatId: String): List<Int> = emptyList()
        override suspend fun getMessagesByIds(groupChatId: String, ids: Set<Int>): List<GroupMessage> = emptyList()
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

    private class FakeMessageRepository : ltd.evilcorp.domain.features.chat.repository.IMessageRepository {
        override suspend fun add(message: ltd.evilcorp.domain.features.chat.model.Message) {}
        override fun get(conversation: String): Flow<List<ltd.evilcorp.domain.features.chat.model.Message>> = flowOf(emptyList())
        override suspend fun getPending(conversation: String): List<ltd.evilcorp.domain.features.chat.model.Message> = emptyList()
        override suspend fun setCorrelationId(id: Long, correlationId: Int) {}
        override suspend fun delete(conversation: String) {}
        override suspend fun deleteMessage(id: Long) {}
        override suspend fun setReceipt(conversation: String, correlationId: Int, timestamp: Long) {}
        override suspend fun exists(conversation: String, message: String): Boolean = false
    }

    private class FakeConnectionScheduler : ltd.evilcorp.domain.features.group.IGroupConnectionScheduler {
        override fun cancelReconnect(chatId: String) {}
        override fun stopReconnect(chatId: String) {}
        override fun scheduleAutoReconnect(chatId: String, groupNumber: Int) {}
        override fun reconnectAll() {}
        override fun isBootstrapFriend(pk: String): Boolean = false
    }

    private class FakeTox : ITox {
        override val toxId: ltd.evilcorp.domain.core.network.ToxID get() = throw UnsupportedOperationException()
        override val publicKey: PublicKey = PublicKey("OUR_PK")
        override var nospam: Int get() = 0; set(_) {}
        override var started: Boolean = true
        override var isBootstrapNeeded: Boolean get() = false; set(_) {}
        override val password: String? get() = null

        val reconnectedGroups = mutableListOf<Int>()
        val bootstrapFriendsAdded = mutableListOf<PublicKey>()
        val contactsDeleted = mutableListOf<PublicKey>()
        var chatList = intArrayOf()
        val joinDirectCalledWith = mutableListOf<Pair<ByteArray, String>>()
        val chatIdMock = mutableMapOf<Int, ByteArray>()

        override fun changePassword(new: String?) {}
        override fun stop() {}
        override fun getContacts(): List<Pair<PublicKey, Int>> = emptyList()
        override fun acceptFriendRequest(publicKey: PublicKey) {}
        override fun startFileTransfer(pk: PublicKey, fileNumber: Int) {}
        override fun stopFileTransfer(pk: PublicKey, fileNumber: Int) {}
        override fun sendFile(pk: PublicKey, fileKind: ltd.evilcorp.domain.features.transfer.model.FileKind, fileSize: Long, fileName: String): Int = 0
        override fun sendFileChunk(pk: PublicKey, fileNo: Int, pos: Long, data: ByteArray): Result<Unit> = Result.success(Unit)
        override fun getName(): String = "Me"
        override fun setName(name: String) {}
        override fun getStatusMessage(): String = ""
        override fun setStatusMessage(statusMessage: String) {}
        override fun addContact(toxId: ltd.evilcorp.domain.core.network.ToxID, message: String) {}
        override fun deleteContact(publicKey: PublicKey) {
            contactsDeleted.add(publicKey)
        }
        override fun sendMessage(publicKey: PublicKey, message: String, type: ltd.evilcorp.domain.features.chat.model.MessageType): Int = 0
        override fun getSaveData(): ByteArray = byteArrayOf()
        override fun setTyping(publicKey: PublicKey, typing: Boolean): Boolean = true
        override fun friendGetTyping(publicKey: PublicKey): Boolean = false
        override fun getFriendNumber(publicKey: PublicKey): Int =
            if (bootstrapFriendsAdded.contains(publicKey)) 0 else -1
        override fun getFriendPublicKey(friendNumber: Int): PublicKey? = null
        override fun friendGetLastOnline(publicKey: PublicKey): Long = 0
        override fun getStatus(): UserStatus = UserStatus.None
        override fun setStatus(status: UserStatus) {}
        override fun sendLosslessPacket(pk: PublicKey, packet: ByteArray): Boolean = true
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
        override fun groupGetChatId(groupNumber: Int): ByteArray? = chatIdMock[groupNumber]
        override fun groupSetPassword(groupNumber: Int, password: ByteArray?): Boolean = true
        override fun groupGetPassword(groupNumber: Int): ByteArray? = null
        override fun groupPeerGetName(groupNumber: Int, peerId: Int): ByteArray? = null
        override fun groupPeerGetPublicKey(groupNumber: Int, peerId: Int): ByteArray? = null
        override fun groupSelfGetPeerId(groupNumber: Int): Int = 0
        override fun groupSelfGetRole(groupNumber: Int): ToxGroupRole = ToxGroupRole.USER
        override fun groupInviteSend(groupNumber: Int, friendNumber: Int): Boolean = true
        
        override fun groupJoinDirect(chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int {
            joinDirectCalledWith.add(Pair(chatId, selfName.decodeToString()))
            return 101
        }
        
        override fun groupReconnect(groupNumber: Int): Boolean {
            reconnectedGroups.add(groupNumber)
            return true
        }
        
        override fun addFriendNoRequest(publicKey: PublicKey): Int {
            bootstrapFriendsAdded.add(publicKey)
            return 0
        }
        
        override fun groupGetChatlist(): IntArray = chatList
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
