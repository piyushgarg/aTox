package ltd.evilcorp.core.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.entity.FriendRequestEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FriendRequestDaoTest {

    private lateinit var db: Database
    private lateinit var dao: ltd.evilcorp.core.db.dao.FriendRequestDao

    private val testRequest = FriendRequestEntity(
        publicKey = "friend123Key",
        message = "Add me please!"
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, Database::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.friendRequestDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSave_and_load() = runTest {
        assertNull(dao.load(testRequest.publicKey).first())
        
        dao.save(testRequest)
        val loaded = dao.load(testRequest.publicKey).first()
        assertEquals(testRequest, loaded)
    }

    @Test
    fun testDelete() = runTest {
        dao.save(testRequest)
        assertEquals(1, dao.count())

        dao.delete(testRequest)
        assertEquals(0, dao.count())
        assertNull(dao.load(testRequest.publicKey).first())
    }

    @Test
    fun testLoadAll_and_count() = runTest {
        assertEquals(0, dao.count())
        assertTrue(dao.loadAll().first().isEmpty())

        val secondRequest = FriendRequestEntity("friend456Key", "Hello!")
        dao.save(testRequest)
        dao.save(secondRequest)

        assertEquals(2, dao.count())
        val all = dao.loadAll().first()
        assertEquals(2, all.size)
        assertTrue(all.contains(testRequest))
        assertTrue(all.contains(secondRequest))
    }
}
