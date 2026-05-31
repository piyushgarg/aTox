package ltd.evilcorp.core.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.dao.FakeFileTransferDao
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.transfer.model.FT_NOT_STARTED
import ltd.evilcorp.domain.features.transfer.model.FT_REJECTED
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FileTransferRepositoryImplTest {

    private lateinit var dao: FakeFileTransferDao
    private lateinit var repository: FileTransferRepositoryImpl

    private val testFt = FileTransfer(
        publicKey = "1234",
        fileNumber = 10,
        fileKind = 1,
        fileSize = 5000L,
        fileName = "photo.jpg",
        outgoing = true,
        progress = FT_NOT_STARTED,
        destination = "/storage/photo.jpg"
    )

    @BeforeTest
    fun setUp() {
        dao = FakeFileTransferDao()
        repository = FileTransferRepositoryImpl(dao)
    }

    @Test
    fun testAdd_and_getById() = runTest {
        val generatedIdLong = repository.add(testFt)
        val generatedId = generatedIdLong.toInt()

        val loaded = repository.get(generatedId).first()
        assertEquals(testFt.fileName, loaded.fileName)
        assertEquals(generatedId, loaded.id)
    }

    @Test
    fun testGetByPublicKey_returnsCorrectList() = runTest {
        repository.add(testFt)
        val second = testFt.copy(fileNumber = 11, fileName = "doc.pdf")
        repository.add(second)

        val list = repository.get(testFt.publicKey).first()
        assertEquals(2, list.size)
        assertTrue(list.any { it.fileName == "photo.jpg" })
        assertTrue(list.any { it.fileName == "doc.pdf" })
    }

    @Test
    fun testDelete_removesTransfer() = runTest {
        val id = repository.add(testFt).toInt()
        assertEquals(1, dao.loadAllBlocking().size)

        repository.delete(id)
        assertEquals(0, dao.loadAllBlocking().size)
    }

    @Test
    fun testSetDestination() = runTest {
        val id = repository.add(testFt).toInt()
        repository.setDestination(id, "/new/path.jpg")

        val loaded = repository.get(id).first()
        assertEquals("/new/path.jpg", loaded.destination)
    }

    @Test
    fun testUpdateProgress() = runTest {
        val id = repository.add(testFt).toInt()
        repository.updateProgress(id, 2500L)

        val loaded = repository.get(id).first()
        assertEquals(2500L, loaded.progress)
    }

    @Test
    fun testResetTransientData_setsUnfinishedTransfersToRejected() = runTest {
        val completeFt = testFt.copy(progress = 5000L) // size is 5000L
        val incompleteFt = testFt.copy(progress = 1000L) // size is 5000L

        val idComplete = repository.add(completeFt).toInt()
        val idIncomplete = repository.add(incompleteFt).toInt()

        repository.resetTransientData()

        val loadedComplete = repository.get(idComplete).first()
        val loadedIncomplete = repository.get(idIncomplete).first()

        assertEquals(5000L, loadedComplete.progress) // complete stays complete
        assertEquals(FT_REJECTED, loadedIncomplete.progress) // incomplete becomes rejected
    }
}
