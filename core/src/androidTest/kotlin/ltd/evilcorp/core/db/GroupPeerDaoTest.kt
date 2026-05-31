package ltd.evilcorp.core.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.entity.GroupPeerEntity
import ltd.evilcorp.domain.features.contacts.model.UserStatus
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
class GroupPeerDaoTest {

    private lateinit var db: Database
    private lateinit var dao: ltd.evilcorp.core.db.dao.GroupPeerDao

    private val testPeer = GroupPeerEntity(
        groupChatId = "group123",
        peerId = 42,
        name = "Bob Marley",
        publicKey = "bobPublicKeyString",
        role = "User",
        isOurselves = false,
        status = UserStatus.None
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, Database::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.groupPeerDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSave_and_load() = runTest {
        assertNull(dao.load(testPeer.groupChatId, testPeer.peerId).first())
        
        dao.save(testPeer)
        val loaded = dao.load(testPeer.groupChatId, testPeer.peerId).first()
        assertEquals(testPeer, loaded)
    }

    @Test
    fun testUpdate() = runTest {
        dao.save(testPeer)

        val updated = testPeer.copy(name = "Bob", role = "Admin")
        dao.update(updated)

        val loaded = dao.load(testPeer.groupChatId, testPeer.peerId).first()
        assertEquals(updated, loaded)
    }

    @Test
    fun testDelete() = runTest {
        dao.save(testPeer)
        assertEquals(1, dao.countForGroupDirect(testPeer.groupChatId))

        dao.delete(testPeer)
        assertEquals(0, dao.countForGroupDirect(testPeer.groupChatId))
    }

    @Test
    fun testLoadAllForGroup() = runTest {
        val p1 = testPeer.copy(peerId = 1)
        val p2 = testPeer.copy(peerId = 2)
        dao.save(p1)
        dao.save(p2)

        val all = dao.loadAllForGroup(testPeer.groupChatId).first()
        assertEquals(2, all.size)
        assertTrue(all.contains(p1))
        assertTrue(all.contains(p2))
    }

    @Test
    fun testGetPeerNameDirect() = runTest {
        assertNull(dao.getPeerNameDirect(testPeer.groupChatId, testPeer.peerId))

        dao.save(testPeer)
        assertEquals("Bob Marley", dao.getPeerNameDirect(testPeer.groupChatId, testPeer.peerId))
    }

    @Test
    fun testPeerExistsDirect() = runTest {
        assertEquals(0, dao.peerExistsDirect(testPeer.groupChatId, testPeer.peerId))

        dao.save(testPeer)
        assertEquals(1, dao.peerExistsDirect(testPeer.groupChatId, testPeer.peerId))
    }

    @Test
    fun testPeerExistsByPublicKeyDirect() = runTest {
        assertEquals(0, dao.peerExistsByPublicKeyDirect(testPeer.groupChatId, testPeer.publicKey))

        dao.save(testPeer)
        assertEquals(1, dao.peerExistsByPublicKeyDirect(testPeer.groupChatId, testPeer.publicKey))
    }

    @Test
    fun testLoadByPublicKey() = runTest {
        assertNull(dao.loadByPublicKey(testPeer.groupChatId, testPeer.publicKey).first())

        dao.save(testPeer)
        val loaded = dao.loadByPublicKey(testPeer.groupChatId, testPeer.publicKey).first()
        assertEquals(testPeer, loaded)
    }

    @Test
    fun testDeleteByPublicKey() = runTest {
        dao.save(testPeer)
        assertEquals(1, dao.countForGroupDirect(testPeer.groupChatId))

        dao.deleteByPublicKey(testPeer.groupChatId, testPeer.publicKey)
        assertEquals(0, dao.countForGroupDirect(testPeer.groupChatId))
    }

    @Test
    fun testSetters() = runTest {
        dao.save(testPeer)

        dao.setName(testPeer.groupChatId, testPeer.peerId, "New Name")
        dao.setRole(testPeer.groupChatId, testPeer.peerId, "Admin")
        dao.setStatus(testPeer.groupChatId, testPeer.peerId, UserStatus.Busy)

        val loaded = dao.load(testPeer.groupChatId, testPeer.peerId).first()
        assertNotNull(loaded)
        assertEquals("New Name", loaded.name)
        assertEquals("Admin", loaded.role)
        assertEquals(UserStatus.Busy, loaded.status)
    }

    @Test
    fun testDeleteByPeerId() = runTest {
        dao.save(testPeer)
        assertEquals(1, dao.countForGroupDirect(testPeer.groupChatId))

        dao.deleteByPeerId(testPeer.groupChatId, testPeer.peerId)
        assertEquals(0, dao.countForGroupDirect(testPeer.groupChatId))
    }

    @Test
    fun testDeleteAllForGroup() = runTest {
        val p1 = testPeer.copy(peerId = 1)
        val p2 = testPeer.copy(peerId = 2)
        dao.save(p1)
        dao.save(p2)
        assertEquals(2, dao.countForGroupDirect(testPeer.groupChatId))

        dao.deleteAllForGroup(testPeer.groupChatId)
        assertEquals(0, dao.countForGroupDirect(testPeer.groupChatId))
    }

    @Test
    fun testCountForGroupFlow() = runTest {
        dao.save(testPeer)
        val count = dao.countForGroup(testPeer.groupChatId).first()
        assertEquals(1, count)
    }
}
