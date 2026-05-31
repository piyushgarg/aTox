package ltd.evilcorp.core.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.dao.FakeGroupDao
import ltd.evilcorp.core.db.dao.FakeGroupMessageDao
import ltd.evilcorp.core.db.dao.FakeGroupPeerDao
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GroupRepositoryImplTest {

    private lateinit var groupDao: FakeGroupDao
    private lateinit var groupMessageDao: FakeGroupMessageDao
    private lateinit var groupPeerDao: FakeGroupPeerDao
    private lateinit var repository: GroupRepositoryImpl

    private val testGroup = Group(
        chatId = "group_123",
        name = "Tox Developers",
        topic = "Decentralized P2P Chat",
        passwordProtected = false,
        privacyState = GroupPrivacyState.Public,
        peerCount = 0,
        selfPeerId = 1,
        selfRole = "Admin",
        lastMessage = 0L,
        hasUnreadMessages = false,
        draftMessage = "Drafting group message",
        connected = true,
        groupNumber = 10
    )

    private val testMessage = GroupMessage(
        groupChatId = "group_123",
        peerId = 2,
        senderName = "Bob",
        message = "Hello group!",
        sender = Sender.Received,
        type = MessageType.Normal,
        correlationId = 5,
        timestamp = 1000L
    )

    private val testPeer = GroupPeer(
        groupChatId = "group_123",
        peerId = 2,
        name = "Bob",
        publicKey = "BOB_PUBLIC_KEY",
        role = "User",
        isOurselves = false
    )

    @BeforeTest
    fun setUp() {
        groupDao = FakeGroupDao()
        groupMessageDao = FakeGroupMessageDao()
        groupPeerDao = FakeGroupPeerDao()
        repository = GroupRepositoryImpl(groupDao, groupMessageDao, groupPeerDao)
    }

    // ========================================================================
    // Group CRUD & Field Updates
    // ========================================================================

    @Test
    fun testGroupCRUD() = runTest {
        assertFalse(repository.exists(testGroup.chatId))
        
        repository.add(testGroup)
        assertTrue(repository.exists(testGroup.chatId))
        
        val loaded = repository.get(testGroup.chatId).first()
        assertEquals(testGroup, loaded)

        val direct = repository.getDirect(testGroup.chatId)
        assertEquals(testGroup, direct)

        val all = repository.getAll().first()
        assertEquals(1, all.size)
        assertEquals(testGroup, all[0])

        val updated = testGroup.copy(name = "New Group Name")
        repository.update(updated)
        assertEquals("New Group Name", repository.getDirect(testGroup.chatId)?.name)

        repository.delete(updated)
        assertFalse(repository.exists(testGroup.chatId))
    }

    @Test
    fun testDeleteByChatId() = runTest {
        repository.add(testGroup)
        assertTrue(repository.exists(testGroup.chatId))
        repository.deleteByChatId(testGroup.chatId)
        assertFalse(repository.exists(testGroup.chatId))
    }

    @Test
    fun testSetters() = runTest {
        repository.add(testGroup)

        repository.setName(testGroup.chatId, "New Name")
        assertEquals("New Name", repository.getDirect(testGroup.chatId)?.name)

        repository.setTopic(testGroup.chatId, "New Topic")
        assertEquals("New Topic", repository.getDirect(testGroup.chatId)?.topic)

        repository.setPasswordProtected(testGroup.chatId, true)
        assertEquals(true, repository.getDirect(testGroup.chatId)?.passwordProtected)

        repository.setPrivacyState(testGroup.chatId, GroupPrivacyState.Private)
        assertEquals(GroupPrivacyState.Private, repository.getDirect(testGroup.chatId)?.privacyState)

        repository.setPeerCount(testGroup.chatId, 5)
        assertEquals(5, repository.getDirect(testGroup.chatId)?.peerCount)

        repository.setSelfPeerId(testGroup.chatId, 9)
        assertEquals(9, repository.getDirect(testGroup.chatId)?.selfPeerId)

        repository.setSelfRole(testGroup.chatId, "Moderator")
        assertEquals("Moderator", repository.getDirect(testGroup.chatId)?.selfRole)

        repository.setLastMessage(testGroup.chatId, 999L)
        assertEquals(999L, repository.getDirect(testGroup.chatId)?.lastMessage)

        repository.setHasUnreadMessages(testGroup.chatId, true)
        assertEquals(true, repository.getDirect(testGroup.chatId)?.hasUnreadMessages)

        repository.setDraftMessage(testGroup.chatId, "new draft")
        assertEquals("new draft", repository.getDirect(testGroup.chatId)?.draftMessage)

        repository.setConnected(testGroup.chatId, false)
        assertEquals(false, repository.getDirect(testGroup.chatId)?.connected)

        repository.setGroupNumber(testGroup.chatId, 42)
        assertEquals(42, repository.getDirect(testGroup.chatId)?.groupNumber)

        val foundChatId = repository.findChatIdByGroupNumber(42)
        assertEquals(testGroup.chatId, foundChatId)
    }

    // ========================================================================
    // Group Message Operations
    // ========================================================================

    @Test
    fun testAddMessage_and_getMessages() = runTest {
        repository.add(testGroup)
        repository.addMessage(testMessage)

        val messages = repository.getMessages(testGroup.chatId).first()
        assertEquals(1, messages.size)
        // Note: addMessage also updates group's last message time (timestamp)
        assertEquals(testMessage.message, messages[0].message)
        assertTrue(groupDao.loadDirect(testGroup.chatId)!!.lastMessage > 0L)
    }

    @Test
    fun testGetPendingMessages() = runTest {
        val pendingMsg = testMessage.copy(timestamp = 0L)
        groupMessageDao.save(ltd.evilcorp.core.db.entity.GroupMessageEntity.fromDomain(pendingMsg))

        val pending = repository.getPendingMessages(testGroup.chatId)
        assertEquals(1, pending.size)
        assertEquals(pendingMsg.message, pending[0].message)
    }

    @Test
    fun testGetUnsentMessages() = runTest {
        // sender == Sent (ordinal 0), correlationId == -1, type != GroupEvent
        val unsentMsg = testMessage.copy(sender = Sender.Sent, correlationId = -1, type = MessageType.Normal)
        groupMessageDao.save(ltd.evilcorp.core.db.entity.GroupMessageEntity.fromDomain(unsentMsg))

        val unsent = repository.getUnsentMessages(testGroup.chatId)
        assertEquals(1, unsent.size)
        assertEquals(unsentMsg.message, unsent[0].message)
    }

    @Test
    fun testSetCorrelationId() = runTest {
        val entity = ltd.evilcorp.core.db.entity.GroupMessageEntity.fromDomain(testMessage)
        groupMessageDao.save(entity)
        val generatedId = groupMessageDao.loadAllBlocking()[0].id

        repository.setCorrelationId(generatedId, 999)

        val updated = groupMessageDao.loadAllBlocking()[0]
        assertEquals(999, updated.correlationId)
    }

    @Test
    fun testDeleteMessages() = runTest {
        repository.addMessage(testMessage)
        assertEquals(1, repository.getMessages(testGroup.chatId).first().size)

        repository.deleteMessages(testGroup.chatId)
        assertEquals(0, repository.getMessages(testGroup.chatId).first().size)
    }

    @Test
    fun testDeleteMessage() = runTest {
        val entity = ltd.evilcorp.core.db.entity.GroupMessageEntity.fromDomain(testMessage)
        groupMessageDao.save(entity)
        val generatedId = groupMessageDao.loadAllBlocking()[0].id

        repository.deleteMessage(generatedId)
        assertEquals(0, groupMessageDao.loadAllBlocking().size)
    }

    @Test
    fun testSetReceipt() = runTest {
        val pendingMsg = testMessage.copy(timestamp = 0L, correlationId = 15)
        groupMessageDao.save(ltd.evilcorp.core.db.entity.GroupMessageEntity.fromDomain(pendingMsg))

        repository.setReceipt(testGroup.chatId, 15, 8888L)

        val updated = groupMessageDao.loadAllBlocking()[0]
        assertEquals(8888L, updated.timestamp)
    }

    @Test
    fun testExistsByCorrelationId() = runTest {
        assertFalse(repository.existsByCorrelationId(testGroup.chatId, 5))
        
        repository.addMessage(testMessage)
        assertTrue(repository.existsByCorrelationId(testGroup.chatId, 5))
    }

    @Test
    fun testGetMessageIds() = runTest {
        repository.addMessage(testMessage)
        val ids = repository.getMessageIds(testGroup.chatId)
        assertEquals(listOf(5), ids)
    }

    @Test
    fun testGetMessagesByIds() = runTest {
        repository.addMessage(testMessage)
        val list = repository.getMessagesByIds(testGroup.chatId, setOf(5))
        assertEquals(1, list.size)
        assertEquals(testMessage.message, list[0].message)
    }

    // ========================================================================
    // Group Peer Operations
    // ========================================================================

    @Test
    fun testPeerCRUD() = runTest {
        repository.addPeer(testPeer)

        val peers = repository.getPeers(testGroup.chatId).first()
        assertEquals(1, peers.size)
        assertEquals(testPeer, peers[0])

        val singlePeer = repository.getPeer(testGroup.chatId, 2).first()
        assertEquals(testPeer, singlePeer)

        val directName = repository.getPeerNameDirect(testGroup.chatId, 2)
        assertEquals("Bob", directName)

        assertTrue(repository.peerExistsDirect(testGroup.chatId, 2))
        assertTrue(repository.peerExistsByPublicKey(testGroup.chatId, "BOB_PUBLIC_KEY"))

        val count = repository.peerCount(testGroup.chatId).first()
        assertEquals(1, count)
        assertEquals(1, repository.peerCountDirect(testGroup.chatId))

        val updatedPeer = testPeer.copy(name = "Bob Smith")
        repository.updatePeer(updatedPeer)
        assertEquals("Bob Smith", repository.getPeerNameDirect(testGroup.chatId, 2))

        repository.deletePeer(updatedPeer)
        assertFalse(repository.peerExistsDirect(testGroup.chatId, 2))
    }

    @Test
    fun testDeletePeerById() = runTest {
        repository.addPeer(testPeer)
        assertTrue(repository.peerExistsDirect(testGroup.chatId, 2))
        repository.deletePeerById(testGroup.chatId, 2)
        assertFalse(repository.peerExistsDirect(testGroup.chatId, 2))
    }

    @Test
    fun testDeleteAllPeers() = runTest {
        repository.addPeer(testPeer)
        assertEquals(1, repository.peerCountDirect(testGroup.chatId))
        repository.deleteAllPeers(testGroup.chatId)
        assertEquals(0, repository.peerCountDirect(testGroup.chatId))
    }

    @Test
    fun testDeletePeerByPublicKey() = runTest {
        repository.addPeer(testPeer)
        assertTrue(repository.peerExistsByPublicKey(testGroup.chatId, "BOB_PUBLIC_KEY"))
        repository.deletePeerByPublicKey(testGroup.chatId, "BOB_PUBLIC_KEY")
        assertFalse(repository.peerExistsByPublicKey(testGroup.chatId, "BOB_PUBLIC_KEY"))
    }

    @Test
    fun testSetPeerName() = runTest {
        repository.addPeer(testPeer)
        repository.setPeerName(testGroup.chatId, 2, "Charlie")
        assertEquals("Charlie", repository.getPeerNameDirect(testGroup.chatId, 2))
    }

    @Test
    fun testSetPeerRole() = runTest {
        repository.addPeer(testPeer)
        repository.setPeerRole(testGroup.chatId, 2, "Moderator")
        val peer = repository.getPeer(testGroup.chatId, 2).first()
        assertEquals("Moderator", peer?.role)
    }

    @Test
    fun testSetPeerStatus() = runTest {
        repository.addPeer(testPeer)
        repository.setPeerStatus(testGroup.chatId, 2, UserStatus.Busy)
        val peer = repository.getPeer(testGroup.chatId, 2).first()
        // Note: status is simulated in FakeGroupPeerDao which is why it maps correctly.
        assertEquals(UserStatus.Busy, peer?.status)
    }
}
