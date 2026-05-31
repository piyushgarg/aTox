package ltd.evilcorp.core.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.entity.GroupEntity
import ltd.evilcorp.domain.features.group.model.GroupPrivacyState
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GroupDaoTest {

    private lateinit var db: Database
    private lateinit var dao: ltd.evilcorp.core.db.dao.GroupDao

    private val testGroup = GroupEntity(
        chatId = "group123",
        name = "Android Geeks",
        topic = "Everything about Kotlin and Android",
        passwordProtected = true,
        privacyState = GroupPrivacyState.Private,
        peerCount = 42,
        selfPeerId = 1,
        selfRole = "Admin",
        lastMessage = 5000L,
        hasUnreadMessages = true,
        draftMessage = "WIP",
        connected = true,
        groupNumber = 10
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, Database::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.groupDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSave_and_load() = runTest {
        assertNull(dao.load(testGroup.chatId).first())
        
        dao.save(testGroup)
        val loaded = dao.load(testGroup.chatId).first()
        assertEquals(testGroup, loaded)
    }

    @Test
    fun testUpdate() = runTest {
        dao.save(testGroup)
        
        val updated = testGroup.copy(name = "Kotlin Developers", connected = false)
        dao.update(updated)
        
        val loaded = dao.load(testGroup.chatId).first()
        assertEquals(updated, loaded)
    }

    @Test
    fun testDelete() = runTest {
        dao.save(testGroup)
        assertTrue(dao.exists(testGroup.chatId) > 0)

        dao.delete(testGroup)
        assertNull(dao.load(testGroup.chatId).first())
    }

    @Test
    fun testDeleteByChatId() = runTest {
        dao.save(testGroup)
        assertTrue(dao.exists(testGroup.chatId) > 0)

        dao.deleteByChatId(testGroup.chatId)
        assertNull(dao.load(testGroup.chatId).first())
    }

    @Test
    fun testExists() = runTest {
        assertEquals(0, dao.exists(testGroup.chatId))
        
        dao.save(testGroup)
        assertEquals(1, dao.exists(testGroup.chatId))
    }

    @Test
    fun testLoadDirect() = runTest {
        assertNull(dao.loadDirect(testGroup.chatId))
        
        dao.save(testGroup)
        assertEquals(testGroup, dao.loadDirect(testGroup.chatId))
    }

    @Test
    fun testLoadAll() = runTest {
        assertTrue(dao.loadAll().first().isEmpty())

        val secondGroup = testGroup.copy(chatId = "group456", groupNumber = 11)
        dao.save(testGroup)
        dao.save(secondGroup)

        val all = dao.loadAll().first()
        assertEquals(2, all.size)
        assertTrue(all.contains(testGroup))
        assertTrue(all.contains(secondGroup))
    }

    @Test
    fun testSetters() = runTest {
        dao.save(testGroup)

        dao.setName(testGroup.chatId, "New Name")
        dao.setTopic(testGroup.chatId, "New Topic")
        dao.setPasswordProtected(testGroup.chatId, false)
        dao.setPrivacyState(testGroup.chatId, GroupPrivacyState.Public)
        dao.setPeerCount(testGroup.chatId, 99)
        dao.setSelfPeerId(testGroup.chatId, 5)
        dao.setSelfRole(testGroup.chatId, "Moderator")
        dao.setLastMessage(testGroup.chatId, 9999L)
        dao.setHasUnreadMessages(testGroup.chatId, false)
        dao.setDraftMessage(testGroup.chatId, "New Draft")
        dao.setConnected(testGroup.chatId, false)
        dao.setGroupNumber(testGroup.chatId, 25)

        val loaded = dao.loadDirect(testGroup.chatId)
        assertNotNull(loaded)
        assertEquals("New Name", loaded.name)
        assertEquals("New Topic", loaded.topic)
        assertFalse(loaded.passwordProtected)
        assertEquals(GroupPrivacyState.Public, loaded.privacyState)
        assertEquals(99, loaded.peerCount)
        assertEquals(5, loaded.selfPeerId)
        assertEquals("Moderator", loaded.selfRole)
        assertEquals(9999L, loaded.lastMessage)
        assertFalse(loaded.hasUnreadMessages)
        assertEquals("New Draft", loaded.draftMessage)
        assertFalse(loaded.connected)
        assertEquals(25, loaded.groupNumber)
    }

    @Test
    fun testFindChatIdByGroupNumber() = runTest {
        assertNull(dao.findChatIdByGroupNumber(10))

        dao.save(testGroup)
        assertEquals(testGroup.chatId, dao.findChatIdByGroupNumber(10))
    }
}
