package ltd.evilcorp.core.tox

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.GroupDataRepositories
import ltd.evilcorp.domain.features.group.GroupInvite
import ltd.evilcorp.domain.features.group.GroupManager
import ltd.evilcorp.domain.features.group.IGroupSessionRegistry
import ltd.evilcorp.domain.features.group.model.Group
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.domain.features.group.GroupToxServices
import ltd.evilcorp.domain.features.group.GroupSessionCoordinator
import ltd.evilcorp.domain.features.group.GroupServices
import ltd.evilcorp.domain.features.group.GroupConnectionService
import ltd.evilcorp.domain.features.group.GroupMessagingService
import ltd.evilcorp.domain.features.group.IGroupConnectionScheduler
import org.junit.Test
import javax.inject.Provider
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GroupConnectionSchedulerImplTest {



    private class FakeGroupRepository : IGroupRepository {
        val groups = mutableMapOf<String, Group>()
        val setConnectedCalled = mutableListOf<Pair<String, Boolean>>()
        val setGroupNumberCalled = mutableListOf<Pair<String, Int>>()

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
            setConnectedCalled.add(Pair(chatId, connected))
            groups[chatId]?.let { groups[chatId] = it.copy(connected = connected) }
        }
        
        override suspend fun setGroupNumber(chatId: String, groupNumber: Int) {
            setGroupNumberCalled.add(Pair(chatId, groupNumber))
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
        override fun getPeers(groupChatId: String): Flow<List<GroupPeer>> = flowOf(emptyList())
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
        override fun get(publicKey: String): Flow<Contact?> = flowOf(null)
        override fun getAll(): Flow<List<Contact>> = flowOf(emptyList())
        override suspend fun exists(publicKey: String): Boolean = false
        override suspend fun add(contact: Contact) {}
        override suspend fun update(contact: Contact) {}
        override suspend fun delete(contact: Contact) {}
        override suspend fun resetTransientData() {}
        override suspend fun setName(publicKey: String, name: String) {}
        override suspend fun setStatusMessage(publicKey: String, statusMessage: String) {}
        override suspend fun setLastMessage(publicKey: String, lastMessage: Long) {}
        override suspend fun setUserStatus(publicKey: String, status: UserStatus) {}
        override suspend fun setConnectionStatus(publicKey: String, status: ltd.evilcorp.domain.features.contacts.model.ConnectionStatus) {}
        override suspend fun setTyping(publicKey: String, typing: Boolean) {}
        override suspend fun setAvatarUri(publicKey: String, uri: String) {}
        override suspend fun setHasUnreadMessages(publicKey: String, anyUnread: Boolean) {}
        override suspend fun setDraftMessage(publicKey: String, draft: String) {}
        override suspend fun setLastOnline(publicKey: String, lastOnline: Long) {}
    }



    private class FakeGroupSessionRegistry : IGroupSessionRegistry {
        override var activeGroup: String = ""
        override val pendingInvite = MutableStateFlow<GroupInvite?>(null)
        override val connectionStatuses = MutableStateFlow<Map<String, GroupConnectionStatus>>(emptyMap())
        
        override fun setPendingInvite(invite: GroupInvite?) {
            pendingInvite.value = invite
        }
        
        override fun setConnectionStatus(chatId: String, status: GroupConnectionStatus) {
            val map = connectionStatuses.value.toMutableMap()
            map[chatId] = status
            connectionStatuses.value = map
        }
        
        override fun removeConnectionStatus(chatId: String) {
            val map = connectionStatuses.value.toMutableMap()
            map.remove(chatId)
            connectionStatuses.value = map
        }

        override fun clear() {
            activeGroup = ""
            pendingInvite.value = null
            connectionStatuses.value = emptyMap()
        }
    }

    @Test
    fun testCancelAndStopReconnect() = runTest {
        val testScope = backgroundScope
        val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)
        val tox = FakeTox()
        val groupRepository = FakeGroupRepository()
        val contactRepository = FakeContactRepository()
        val messageRepository = FakeMessageRepository()
        val sessionRegistry = FakeGroupSessionRegistry()

        val groupDataRepositories = GroupDataRepositories(groupRepository, contactRepository, messageRepository)
        val chatManager = ChatManager(testScope, contactRepository, messageRepository, tox, testDispatcher)
        val toxServices = GroupToxServices(tox, tox)
        
        val fakeScheduler = object : IGroupConnectionScheduler {
            override fun reconnectAll() {}
            override fun scheduleAutoReconnect(chatId: String, groupNumber: Int) {}
            override fun cancelReconnect(chatId: String) {}
            override fun stopReconnect(chatId: String) {}
            override fun isBootstrapFriend(pk: String): Boolean = false
        }
        val sessionCoordinator = GroupSessionCoordinator(fakeScheduler, sessionRegistry)
        
        val connectionService = GroupConnectionService(testScope, sessionCoordinator, groupRepository, toxServices, testDispatcher)
        val groupEventBus = ltd.evilcorp.domain.features.group.GroupEventBus()
        val messagingService = GroupMessagingService(
            testScope,
            groupDataRepositories,
            toxServices,
            sessionCoordinator,
            groupEventBus,
            testDispatcher
        )
        
        val services = GroupServices(connectionService, messagingService)

        val groupManager = GroupManager(
            testScope,
            groupDataRepositories,
            chatManager,
            toxServices,
            sessionCoordinator,
            services
        )

        val provider = Provider { groupManager }

        val scheduler = GroupConnectionSchedulerImpl(
            testScope,
            tox,
            groupRepository,
            provider
        )

        // Verify simple initial API
        assertFalse(scheduler.isBootstrapFriend("FAKEPK"))
        
        scheduler.cancelReconnect("group_1")
        scheduler.stopReconnect("group_1")
    }

    @Test
    fun testReconnectAll_triggersReconnections() = runTest {
        val testScope = backgroundScope
        val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)
        val tox = FakeTox()
        val groupRepository = FakeGroupRepository()
        val contactRepository = FakeContactRepository()
        val messageRepository = FakeMessageRepository()
        val sessionRegistry = FakeGroupSessionRegistry()

        val groupDataRepositories = GroupDataRepositories(groupRepository, contactRepository, messageRepository)
        val chatManager = ChatManager(testScope, contactRepository, messageRepository, tox, testDispatcher)
        val toxServices = GroupToxServices(tox, tox)
        
        val fakeScheduler = object : IGroupConnectionScheduler {
            override fun reconnectAll() {}
            override fun scheduleAutoReconnect(chatId: String, groupNumber: Int) {}
            override fun cancelReconnect(chatId: String) {}
            override fun stopReconnect(chatId: String) {}
            override fun isBootstrapFriend(pk: String): Boolean = false
        }
        val sessionCoordinator = GroupSessionCoordinator(fakeScheduler, sessionRegistry)
        
        val connectionService = GroupConnectionService(testScope, sessionCoordinator, groupRepository, toxServices, testDispatcher)
        val groupEventBus = ltd.evilcorp.domain.features.group.GroupEventBus()
        val messagingService = GroupMessagingService(
            testScope,
            groupDataRepositories,
            toxServices,
            sessionCoordinator,
            groupEventBus,
            testDispatcher
        )
        
        val services = GroupServices(connectionService, messagingService)

        val groupManager = GroupManager(
            testScope,
            groupDataRepositories,
            chatManager,
            toxServices,
            sessionCoordinator,
            services
        )

        val provider = Provider { groupManager }

        val scheduler = GroupConnectionSchedulerImpl(
            testScope,
            tox,
            groupRepository,
            provider
        )

        // Set up database groups
        val g1 = Group(
            chatId = "010203", // byte array equivalent to [1, 2, 3] in hex
            name = "Test",
            topic = "",
            passwordProtected = false,
            privacyState = GroupPrivacyState.Public,
            peerCount = 0,
            selfPeerId = 0,
            selfRole = "",
            lastMessage = 0L,
            hasUnreadMessages = false,
            draftMessage = "",
            connected = false,
            groupNumber = 10
        )
        groupRepository.add(g1)

        // Execute reconnectAll
        scheduler.reconnectAll()

        // Wait for coroutine executions
        kotlinx.coroutines.delay(400)

        // Check if group reconnect API was called
        assertTrue(tox.reconnectedGroups.contains(10))

        // Clean up coroutine to avoid leaks!
        scheduler.stopReconnect(g1.chatId)
    }
}
