package ltd.evilcorp.domain.features.group.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.group.*
import ltd.evilcorp.domain.features.group.model.*
import ltd.evilcorp.domain.fakes.*
import kotlin.test.*

class GroupUseCasesTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val groupRepo = FakeGroupRepository()
    private val contactRepo = FakeContactRepository()
    private val messageRepo = FakeMessageRepository()
    private val groupDataRepos = GroupDataRepositories(groupRepo, contactRepo, messageRepo)

    private val tox = FakeTox()
    private val chatManager = ChatManager(
        scope = scope,
        contactRepository = contactRepo,
        messageRepository = messageRepo,
        tox = tox,
        ioDispatcher = Dispatchers.Unconfined
    )

    private val toxServices = GroupToxServices(tox, tox)

    private val connectionScheduler = FakeGroupConnectionScheduler()
    private val sessionRegistry = FakeGroupSessionRegistry()
    private val sessionCoordinator = GroupSessionCoordinator(connectionScheduler, sessionRegistry)

    private val groupConnectionService = GroupConnectionService(scope, sessionCoordinator, groupRepo, toxServices, Dispatchers.Unconfined)
    private val groupEventBus = GroupEventBus()
    private val groupMessagingService = GroupMessagingService(scope, groupDataRepos, toxServices, sessionCoordinator, groupEventBus, Dispatchers.Unconfined)
    private val groupServices = GroupServices(groupConnectionService, groupMessagingService)

    private val groupManager = GroupManager(
        scope = scope,
        repositories = groupDataRepos,
        chatManager = chatManager,
        toxServices = toxServices,
        sessionCoordinator = sessionCoordinator,
        services = groupServices
    )

    @Test
    fun `CreateGroupUseCase creates group and adds self peer`() = runTest {
        val useCase = CreateGroupUseCase(groupConnectionService, groupManager)
        tox.setName("Alice")

        val groupNumber = useCase.execute("My Public Chat", GroupPrivacyState.Public)

        assertEquals(0, groupNumber)
        val allGroups = groupManager.getAll().first()
        assertEquals(1, allGroups.size)
        val createdGroup = allGroups[0]
        assertEquals("My Public Chat", createdGroup.name)
        assertEquals(GroupPrivacyState.Public, createdGroup.privacyState)

        val peers = groupRepo.getPeers(createdGroup.chatId).first()
        assertEquals(1, peers.size)
        val selfPeer = peers[0]
        assertEquals("Alice", selfPeer.name)
        assertTrue(selfPeer.isOurselves)
    }

    @Test
    fun `JoinGroupUseCase joins group by friend invite`() = runTest {
        val useCase = JoinGroupUseCase(groupConnectionService, groupManager)
        tox.setName("Alice")

        val invite = GroupInvite(5, byteArrayOf(1, 2), "Group Name")
        val groupNumber = useCase.execute(JoinAction.ByPendingInvite(invite))

        assertEquals(0, groupNumber)
        val allGroups = groupManager.getAll().first()
        assertEquals(1, allGroups.size)
        val createdGroup = allGroups[0]
        assertEquals("Unknown Group", createdGroup.name)
        assertEquals(0, connectionScheduler.scheduledAutoReconnects[createdGroup.chatId])
    }

    @Test
    fun `LeaveGroupUseCase leaves group and deletes peers`() = runTest {
        val useCase = LeaveGroupUseCase(groupConnectionService)
        val chatId = "test_chat_id"
        groupRepo.add(Group(chatId = chatId, name = "Temp Group", groupNumber = 1))
        groupRepo.addPeer(GroupPeer(groupChatId = chatId, peerId = 1, name = "Bob", publicKey = "pk"))

        useCase.execute(chatId)

        assertFalse(groupRepo.exists(chatId))
        val peers = groupRepo.getPeers(chatId).first()
        assertTrue(peers.isEmpty())
    }

    @Test
    fun `InviteFriendToGroupUseCase sends invite and logs in message repo`() = runTest {
        val useCase = InviteFriendToGroupUseCase(groupMessagingService)
        val chatId = "test_chat_id"
        val friendPk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        groupRepo.add(Group(chatId = chatId, name = "My Group", groupNumber = 0))
        contactRepo.add(Contact(friendPk.string(), connectionStatus = ConnectionStatus.UDP))

        useCase.execute(chatId, friendPk.string())

        val messages = messageRepo.get(friendPk.string().lowercase()).first()
        assertEquals(1, messages.size)
        assertEquals("[GROUP_INVITE:My Group|test_chat_id]", messages[0].message)
    }

    @Test
    fun `SendGroupMessageUseCase sends message successfully`() = runTest {
        val useCase = SendGroupMessageUseCase(groupMessagingService, groupRepo)
        val chatId = "test_chat_id"
        groupRepo.add(Group(chatId = chatId, name = "Group", groupNumber = 0, selfPeerId = 1))
        sessionRegistry.setConnectionStatus(chatId, GroupConnectionStatus.Connected)

        useCase.execute(chatId, "Hello Group!")

        val messages = groupRepo.getMessages(chatId).first()
        assertEquals(1, messages.size)
        assertEquals("Hello Group!", messages[0].message)
    }

    @Test
    fun `GetGroupPeersUseCase retrieves group peers flow`() = runTest {
        val useCase = GetGroupPeersUseCase(groupRepo)
        val chatId = "test_chat_id"
        val peer = GroupPeer(groupChatId = chatId, peerId = 1, name = "Bob", publicKey = "pk")
        groupRepo.addPeer(peer)

        val peers = useCase.execute(chatId).first()
        assertEquals(1, peers.size)
        assertEquals(peer, peers[0])
    }

    @Test
    fun `SetActiveGroupUseCase sets active group and clears unread status`() = runTest {
        val useCase = SetActiveGroupUseCase(groupManager)
        val chatId = "test_chat_id"
        groupRepo.add(Group(chatId = chatId, name = "Group", hasUnreadMessages = true))

        useCase.execute(chatId)

        assertEquals(chatId, groupManager.activeGroup)
        val group = groupRepo.get(chatId).first()
        assertNotNull(group)
        assertFalse(group.hasUnreadMessages)
    }

    @Test
    fun `SetGroupDraftUseCase updates draft message in repository`() = runTest {
        val useCase = SetGroupDraftUseCase(groupRepo)
        val chatId = "test_chat_id"
        groupRepo.add(Group(chatId = chatId, name = "Group", draftMessage = ""))

        useCase.execute(chatId, "Draft text")

        val group = groupRepo.get(chatId).first()
        assertEquals("Draft text", group?.draftMessage)
    }

    @Test
    fun `GetGroupMessagesUseCase retrieves group messages`() = runTest {
        val useCase = GetGroupMessagesUseCase(groupRepo)
        val chatId = "test_chat_id"
        val msg = GroupMessage(
            groupChatId = chatId, peerId = 1, senderName = "Bob", message = "Hi",
            sender = Sender.Received, type = MessageType.Normal, correlationId = 0, timestamp = 1000L
        )
        groupRepo.addMessage(msg)

        val messages = useCase.execute(chatId).first()
        assertEquals(1, messages.size)
        assertEquals(msg, messages[0])
    }

    @Test
    fun `ClearGroupHistoryUseCase clears group messages`() = runTest {
        val useCase = ClearGroupHistoryUseCase(groupRepo)
        val chatId = "test_chat_id"
        groupRepo.addMessage(
            GroupMessage(
                groupChatId = chatId, peerId = 1, senderName = "Bob", message = "Hi",
                sender = Sender.Received, type = MessageType.Normal, correlationId = 0, timestamp = 1000L
            )
        )

        useCase.execute(chatId)

        val messages = groupRepo.getMessages(chatId).first()
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `DeleteGroupMessageUseCase deletes specific message`() = runTest {
        val useCase = DeleteGroupMessageUseCase(groupRepo)
        val msg = GroupMessage(
            groupChatId = "chat_id", peerId = 1, senderName = "Bob", message = "Hi",
            sender = Sender.Received, type = MessageType.Normal, correlationId = 0, timestamp = 1000L,
            id = 123L
        )
        groupRepo.addMessage(msg)

        useCase.execute(123L)

        val messages = groupRepo.getMessages("chat_id").first()
        assertTrue(messages.isEmpty())
    }

    @Test
    fun `GetGroupsUseCase retrieves all groups`() = runTest {
        val useCase = GetGroupsUseCase(groupManager)
        val chatId = "test_chat_id"
        val g = Group(chatId = chatId, name = "Group")
        groupRepo.add(g)

        val groups = useCase.execute().first()
        assertEquals(1, groups.size)
        assertEquals(g, groups[0])
    }

    @Test
    fun `GetGroupChatUseCase retrieves specific group`() = runTest {
        val useCase = GetGroupChatUseCase(groupManager)
        val chatId = "test_chat_id"
        val g = Group(chatId = chatId, name = "Group")
        groupRepo.add(g)

        val retrieved = useCase.execute(chatId).first()
        assertEquals(g, retrieved)
    }

    @Test
    fun `GetGroupConnectionStatusUseCase and GetGroupConnectionStatusesUseCase retrieve connection status`() = runTest {
        val statusUseCase = GetGroupConnectionStatusUseCase(groupManager)
        val statusesUseCase = GetGroupConnectionStatusesUseCase(groupManager)
        val chatId = "test_chat_id"
        sessionRegistry.setConnectionStatus(chatId, GroupConnectionStatus.Connecting)

        val singleStatus = statusUseCase.execute(chatId).first()
        assertEquals(GroupConnectionStatus.Connecting, singleStatus)

        val allStatuses = statusesUseCase.execute().first()
        assertEquals(GroupConnectionStatus.Connecting, allStatuses[chatId])
    }

    @Test
    fun `AcceptGroupInviteUseCase accepts pending invite and joins group`() = runTest {
        val useCase = AcceptGroupInviteUseCase(sessionRegistry, tox, groupConnectionService)
        val invite = GroupInvite(0, byteArrayOf(1, 2), "Group Name")
        sessionRegistry.setPendingInvite(invite)
        tox.setName("Alice")

        useCase.execute()
        assertNull(sessionRegistry.pendingInvite.value)
    }

    @Test
    fun `DeclineGroupInviteUseCase declines pending invite`() = runTest {
        val useCase = DeclineGroupInviteUseCase(groupConnectionService, sessionRegistry)
        val invite = GroupInvite(0, byteArrayOf(1, 2), "Group Name")
        sessionRegistry.setPendingInvite(invite)

        useCase.execute()
        assertNull(sessionRegistry.pendingInvite.value)
    }

    @Test
    fun `ReconnectGroupsUseCase triggers reconnect on scheduler`() = runTest {
        val useCase = ReconnectGroupsUseCase(groupConnectionService)
        useCase.execute()
        assertTrue(connectionScheduler.reconnectedAllCalled)
    }

    @Test
    fun `GetGroupInviteUseCase retrieves pending invite`() = runTest {
        val useCase = GetGroupInviteUseCase(sessionRegistry)
        val invite = GroupInvite(0, byteArrayOf(1, 2), "Group Name")
        sessionRegistry.setPendingInvite(invite)

        val retrieved = useCase.execute().first()
        assertEquals(invite, retrieved)
    }
}
