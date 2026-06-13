package ltd.evilcorp.domain.features.transfer.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.transfer.*
import ltd.evilcorp.domain.features.transfer.model.FT_NOT_STARTED
import ltd.evilcorp.domain.features.transfer.model.FT_STARTED
import ltd.evilcorp.domain.features.transfer.model.FileKind
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.fakes.*
import ltd.evilcorp.domain.core.io.IInputStream
import kotlin.test.*

class FileTransferUseCasesTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val platformHelper = FakeFileTransferPlatformHelper()
    private val storageHelper = FakeFileStorageHelper()
    private val storageCoordinator = FileStoragePlatformCoordinator(platformHelper, storageHelper)

    private val contactRepo = FakeContactRepository()
    private val messageRepo = FakeMessageRepository()
    private val transferRepo = FakeFileTransferRepository()
    private val settingsRepo = FakeUserSettingsRepository()
    private val repositories = FileTransferRepositories(contactRepo, messageRepo, transferRepo, settingsRepo)

    private val sessionRegistry = FakeFileTransferSessionRegistry()
    private val tox = FakeTox()

    private val fileTransferManager = FileTransferManager(
        scope,
        storageCoordinator,
        repositories,
        tox,
        tox,
        sessionRegistry,
        Dispatchers.Unconfined
    )

    private val useCase = ManageFileTransferUseCase(fileTransferManager, Dispatchers.Unconfined)

    @Test
    fun `ManageFileTransferUseCase handles Accept action`() = runTest {
        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val ft = FileTransfer(
            publicKey = pk.string(),
            fileNumber = 0,
            fileKind = FileKind.Data.ordinal,
            fileSize = 1024L,
            fileName = "document.pdf",
            outgoing = false,
            progress = FT_NOT_STARTED
        ).apply { id = 12 }
        sessionRegistry.fileTransfers.add(ft)

        useCase.execute(FileTransferAction.Accept(12))

        var attempts = 0
        while (!storageHelper.emptyFileCreated && attempts < 20) {
            delay(50)
            attempts++
        }

        assertTrue(storageHelper.emptyFileCreated, "storageHelper.emptyFileCreated should be true")
        assertEquals(1, sessionRegistry.fileTransfers.size)
    }

    @Test
    fun `ManageFileTransferUseCase handles Reject action`() = runTest {
        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val ft = FileTransfer(
            publicKey = pk.string(),
            fileNumber = 0,
            fileKind = FileKind.Data.ordinal,
            fileSize = 1024L,
            fileName = "document.pdf",
            outgoing = false,
            progress = FT_NOT_STARTED
        ).apply { id = 12 }
        sessionRegistry.fileTransfers.add(ft)

        useCase.execute(FileTransferAction.Reject(12))

        var attempts = 0
        while (sessionRegistry.fileTransfers.isNotEmpty() && attempts < 20) {
            delay(50)
            attempts++
        }

        assertTrue(sessionRegistry.fileTransfers.isEmpty())
    }

    @Test
    fun `ManageFileTransferUseCase handles Delete action`() = runTest {
        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val ft = FileTransfer(
            publicKey = pk.string(),
            fileNumber = 0,
            fileKind = FileKind.Data.ordinal,
            fileSize = 1024L,
            fileName = "document.pdf",
            outgoing = false,
            progress = FT_NOT_STARTED
        ).apply { id = 12 }
        transferRepo.add(ft)

        useCase.execute(FileTransferAction.Delete(12))

        var attempts = 0
        while (transferRepo.get(pk.string()).first().isNotEmpty() && attempts < 20) {
            delay(50)
            attempts++
        }

        val retrieved = transferRepo.get(pk.string()).first()
        assertTrue(retrieved.isEmpty())
    }

    @Test
    fun `ManageFileTransferUseCase handles Create action`() = runTest {
        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        contactRepo.add(Contact(pk.string(), connectionStatus = ConnectionStatus.UDP))

        // Set non-null mock IInputStream to avoid Reject trigger
        platformHelper.streamToReturn = object : IInputStream {
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int = -1
            override fun close() {}
        }

        useCase.execute(FileTransferAction.Create(pk, "content://media/external/files/1"))

        var attempts = 0
        while (sessionRegistry.fileTransfers.isEmpty() && attempts < 20) {
            delay(50)
            attempts++
        }

        assertEquals(1, sessionRegistry.fileTransfers.size)
        val created = sessionRegistry.fileTransfers[0]
        assertEquals(pk.string(), created.publicKey)
        assertEquals("test_file.txt", created.fileName)
        assertTrue(created.outgoing)
    }

    @Test
    fun `FileTransferManager addDataToTransfer updates progress and auto saves`() = runTest {
        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val ft = FileTransfer(
            publicKey = pk.string(),
            fileNumber = 1,
            fileKind = FileKind.Data.ordinal,
            fileSize = 100L,
            fileName = "document.pdf",
            outgoing = false,
            progress = FT_STARTED,
            destination = "file://fake/dest"
        ).apply { id = 15 }
        sessionRegistry.fileTransfers.add(ft)
        transferRepo.add(ft)

        fileTransferManager.addDataToTransfer(pk.string(), 1, 0, byteArrayOf(1, 2, 3, 4, 5))

        // Progress updated by 5 bytes
        val updatedFt = transferRepo.get(15).first()
        assertEquals(FT_STARTED + 5, updatedFt.progress)
    }
}
