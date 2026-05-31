package ltd.evilcorp.core.platform.storage

import ltd.evilcorp.domain.core.io.IInputStream
import ltd.evilcorp.domain.features.transfer.OutgoingFile
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.transfer.Chunk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileTransferSessionRegistryImplTest {

    private lateinit var registry: FileTransferSessionRegistryImpl

    private val fakeInputStream = object : IInputStream {
        override fun read(bytes: ByteArray, offset: Int, length: Int): Int = -1
        override fun close() {}
    }

    @BeforeTest
    fun setUp() {
        registry = FileTransferSessionRegistryImpl()
    }

    @Test
    fun testInitialState() {
        assertTrue(registry.fileTransfers.isEmpty())
        assertTrue(registry.outgoingFiles.isEmpty())
    }

    @Test
    fun testAddFileTransfer() {
        val transfer = FileTransfer(
            publicKey = "publicKey123",
            fileNumber = 1,
            fileKind = 0,
            fileSize = 1024L,
            fileName = "document.pdf",
            outgoing = true
        )
        
        registry.fileTransfers.add(transfer)
        assertEquals(1, registry.fileTransfers.size)
        assertEquals(transfer, registry.fileTransfers[0])
    }

    @Test
    fun testAddOutgoingFile() {
        val key = Pair("friendPublicKey", 42)
        val outgoingFile = OutgoingFile(
            inputStream = fakeInputStream,
            unsentChunks = mutableListOf(Chunk(0L, byteArrayOf(1, 2, 3)))
        )
        
        registry.outgoingFiles[key] = outgoingFile
        assertEquals(1, registry.outgoingFiles.size)
        assertEquals(outgoingFile, registry.outgoingFiles[key])
    }
}
