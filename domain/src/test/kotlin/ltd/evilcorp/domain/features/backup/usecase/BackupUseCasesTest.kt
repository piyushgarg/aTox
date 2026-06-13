package ltd.evilcorp.domain.features.backup.usecase

import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.backup.ExportManager
import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider
import ltd.evilcorp.domain.fakes.FakeMessageRepository
import ltd.evilcorp.domain.fakes.FakePlatformServices
import kotlin.test.*

class BackupUseCasesTest {

    private val messageRepo = FakeMessageRepository()
    private val platformServices = FakePlatformServices()
    private val exportManager = ExportManager(messageRepo, platformServices)

    @Test
    fun `ExportManager generates pretty JSON export of chat messages`() = runTest {
        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val msg = Message(
            publicKey = pk.string().lowercase(),
            message = "Hello!",
            sender = Sender.Sent,
            type = MessageType.Normal,
            correlationId = 0,
            timestamp = 1000L,
            id = 1L
        )
        messageRepo.add(msg)

        val jsonResult = exportManager.generateExportMessagesJString(pk.string().lowercase())
        assertTrue(jsonResult.contains("Hello!"))
        assertTrue(jsonResult.contains("Sent"))
        assertTrue(jsonResult.contains("Normal"))
    }

    @Test
    fun `ExportChatHistoryUseCase delegates to ExportManager`() = runTest {
        val useCase = ExportChatHistoryUseCase(exportManager, kotlinx.coroutines.Dispatchers.Unconfined)
        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val msg = Message(
            publicKey = pk.string().lowercase(),
            message = "Hello UseCase!",
            sender = Sender.Sent,
            type = MessageType.Normal,
            correlationId = 0,
            timestamp = 1000L,
            id = 1L
        )
        messageRepo.add(msg)

        val jsonResult = useCase.execute(pk.string().lowercase())
        assertTrue(jsonResult.contains("Hello UseCase!"))
    }


    @Test
    fun `ExportBackupUseCase zips and optionally encrypts provider data`() = runTest {
        val provider = object : IBackupDataProvider {
            override val id: String = "test_provider"
            override val displayNameRes: Int = 0
            override val descriptionRes: Int = 0
            override suspend fun serialize(outputStream: java.io.OutputStream) {
                outputStream.write("serialized_data".encodeToByteArray())
            }
            override suspend fun deserialize(data: ByteArray) {}
        }
        val useCase = ExportBackupUseCase(listOf(provider))

        val selectedIds = setOf("test_provider")
        
        // Test raw backup export
        val rawResult = useCase.execute(selectedIds)
        assertTrue(rawResult.isNotEmpty())

        val entries = mutableMapOf<String, ByteArray>()
        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(rawResult)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        assertEquals(2, entries.size)
        assertTrue(entries.containsKey("manifest.txt"))
        assertEquals("aTox selective backup\n", entries["manifest.txt"]?.decodeToString())
        assertTrue(entries.containsKey("test_provider.bin"))
        assertEquals("serialized_data", entries["test_provider.bin"]?.decodeToString())

    }

    @Test
    fun `ImportBackupUseCase decrypts and deserializes provider data`() = runTest {
        var deserializedBytes: ByteArray? = null
        val provider = object : IBackupDataProvider {
            override val id: String = "test_provider"
            override val displayNameRes: Int = 0
            override val descriptionRes: Int = 0
            override suspend fun serialize(outputStream: java.io.OutputStream) {}
            override suspend fun deserialize(data: ByteArray) {
                deserializedBytes = data
            }
        }
        val useCase = ImportBackupUseCase(listOf(provider), platformServices)
        
        val mockZipBytes = "zipped_data".encodeToByteArray()
        platformServices.unzippedResult = mapOf("test_provider.bin" to "deserialized_data".encodeToByteArray())

        useCase.execute(mockZipBytes)

        assertNotNull(deserializedBytes)
        assertContentEquals("deserialized_data".encodeToByteArray(), deserializedBytes)
    }
}
