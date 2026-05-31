package ltd.evilcorp.core.platform.backup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.dao.FakeFileTransferDao
import ltd.evilcorp.core.db.entity.FileTransferEntity
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FileTransferBackupHelperImplTest {

    private lateinit var fileTransferDao: FakeFileTransferDao
    private lateinit var backupHelper: FileTransferBackupHelperImpl

    @BeforeTest
    fun setUp() {
        fileTransferDao = FakeFileTransferDao()
        backupHelper = FileTransferBackupHelperImpl(fileTransferDao)
    }

    @Test
    fun testSerializeFileTransfers_returnsAllDomainTransfers() = runTest {
        val transfer1 = FileTransferEntity("user1", 1, 0, 1024L, "doc.pdf", true, 500L, "/path/1").apply { id = 100 }
        val transfer2 = FileTransferEntity("user2", 2, 1, 2048L, "img.jpg", false, 1000L, "/path/2").apply { id = 101 }
        fileTransferDao.save(transfer1)
        fileTransferDao.save(transfer2)

        val serialized = backupHelper.serializeFileTransfers()
        assertEquals(2, serialized.size)

        val first = serialized.first { it.id == 100 }
        assertEquals("user1", first.publicKey)
        assertEquals(1, first.fileNumber)
        assertEquals(0, first.fileKind)
        assertEquals(1024L, first.fileSize)
        assertEquals("doc.pdf", first.fileName)
        assertEquals(true, first.outgoing)
        assertEquals(500L, first.progress)
        assertEquals("/path/1", first.destination)
    }

    @Test
    fun testDeserializeFileTransfers_savesToDatabase() = runTest {
        val domainTransfer = FileTransfer("user3", 3, 0, 3072L, "archive.zip", false, 0L, "/path/3").apply { id = 200 }
        backupHelper.deserializeFileTransfers(listOf(domainTransfer))

        val loaded = fileTransferDao.loadAllBlocking()
        assertEquals(1, loaded.size)
        assertEquals("user3", loaded[0].publicKey)
        assertEquals("archive.zip", loaded[0].fileName)
        assertEquals(200, loaded[0].id)
    }

    @Test
    fun testSetDestination_updatesDestinationInDatabase() = runTest {
        val transfer = FileTransferEntity("user1", 1, 0, 1024L, "doc.pdf", true, 0L, "").apply { id = 50 }
        fileTransferDao.save(transfer)

        backupHelper.setDestination(50, "/final/destination/path")

        val loaded = fileTransferDao.loadAllBlocking()
        assertEquals(1, loaded.size)
        assertEquals("/final/destination/path", loaded[0].destination)
    }
}
