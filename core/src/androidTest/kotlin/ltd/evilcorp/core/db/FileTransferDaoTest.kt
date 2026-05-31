package ltd.evilcorp.core.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.entity.FileTransferEntity
import ltd.evilcorp.domain.features.transfer.model.FT_NOT_STARTED
import ltd.evilcorp.domain.features.transfer.model.FT_REJECTED
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FileTransferDaoTest {

    private lateinit var db: Database
    private lateinit var dao: ltd.evilcorp.core.db.dao.FileTransferDao

    private val testTransfer = FileTransferEntity(
        publicKey = "friend123",
        fileNumber = 2,
        fileKind = 0,
        fileSize = 10000L,
        fileName = "my_video.mp4",
        outgoing = true,
        progress = FT_NOT_STARTED,
        destination = "/storage/downloads/my_video.mp4"
    )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, Database::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.fileTransferDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSave_and_load() = runTest {
        val id = dao.save(testTransfer).toInt()
        
        val loaded = dao.load(id).first()
        assertEquals("friend123", loaded.publicKey)
        assertEquals(2, loaded.fileNumber)
        assertEquals(10000L, loaded.fileSize)
        assertEquals("my_video.mp4", loaded.fileName)
        assertEquals(FT_NOT_STARTED, loaded.progress)
    }

    @Test
    fun testSaveAll_and_loadAllBlocking() = runTest {
        val t1 = testTransfer.copy(fileName = "file1.txt")
        val t2 = testTransfer.copy(fileName = "file2.png")
        dao.saveAll(listOf(t1, t2))

        val loaded = dao.loadAllBlocking()
        assertEquals(2, loaded.size)
        assertTrue(loaded.any { it.fileName == "file1.txt" })
        assertTrue(loaded.any { it.fileName == "file2.png" })
    }

    @Test
    fun testDelete() = runTest {
        val id = dao.save(testTransfer).toInt()
        assertEquals(1, dao.loadAllBlocking().size)

        dao.delete(id)
        assertTrue(dao.loadAllBlocking().isEmpty())
    }

    @Test
    fun testLoadByPublicKey() = runTest {
        val t1 = testTransfer.copy(publicKey = "userA")
        val t2 = testTransfer.copy(publicKey = "userB")
        dao.saveAll(listOf(t1, t2))

        val loadedA = dao.load("userA").first()
        assertEquals(1, loadedA.size)
        assertEquals("userA", loadedA[0].publicKey)
    }

    @Test
    fun testUpdateProgress_and_rejectCheck() = runTest {
        val id = dao.save(testTransfer).toInt()

        // Update progress to 5000L
        dao.updateProgress(id, 5000L)
        assertEquals(5000L, dao.loadAllBlocking()[0].progress)

        // If progress is already rejected, it should not be updated
        // First reject it manually (Room updateProgress has a guard)
        dao.save(dao.loadAllBlocking()[0].copy(progress = FT_REJECTED).apply { this.id = id })
        assertEquals(FT_REJECTED, dao.loadAllBlocking()[0].progress)

        dao.updateProgress(id, 8000L) // this update should be ignored due to where progress != :rejected
        assertEquals(FT_REJECTED, dao.loadAllBlocking()[0].progress)
    }

    @Test
    fun testSetDestination() = runTest {
        val id = dao.save(testTransfer).toInt()
        
        dao.setDestination(id, "/new/final/path.mp4")
        assertEquals("/new/final/path.mp4", dao.loadAllBlocking()[0].destination)
    }

    @Test
    fun testResetTransientData() = runTest {
        // Reset transient data should set all uncompleted file transfers (progress < file_size) to FT_REJECTED
        val completed = testTransfer.copy(progress = 10000L) // 10000L/10000L
        val active = testTransfer.copy(progress = 5000L) // 5000L/10000L
        
        val completedId = dao.save(completed).toInt()
        val activeId = dao.save(active).toInt()

        dao.resetTransientData(FT_REJECTED)

        val all = dao.loadAllBlocking()
        val completedSaved = all.first { it.id == completedId }
        val activeSaved = all.first { it.id == activeId }

        assertEquals(10000L, completedSaved.progress) // completed should remain unchanged
        assertEquals(FT_REJECTED, activeSaved.progress) // incomplete should be marked rejected
    }
}
