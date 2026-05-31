package ltd.evilcorp.core.tox.listener

import ltd.evilcorp.domain.core.network.enums.ToxFileControl
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToxFileEventDispatcherTest {

    @Test
    fun testAllDispatchedFileEvents() {
        val listener = ToxEventListener()
        val dispatcher = ToxFileEventDispatcher(listener)

        var fileRecvCalled = false
        listener.fileRecvHandler = { publicKey, fileNo, kind, size, name ->
            assertEquals("friend_pk", publicKey)
            assertEquals(1, fileNo)
            assertEquals(0, kind)
            assertEquals(1024L, size)
            assertEquals("photo.jpg", name)
            fileRecvCalled = true
        }
        dispatcher.onFileRecv("friend_pk", 1, 0, 1024L, "photo.jpg".toByteArray())
        assertTrue(fileRecvCalled)

        var fileControlCalled = false
        listener.fileRecvControlHandler = { publicKey, fileNo, control ->
            assertEquals("friend_pk", publicKey)
            assertEquals(1, fileNo)
            assertEquals(ToxFileControl.RESUME, control)
            fileControlCalled = true
        }
        dispatcher.onFileRecvControl("friend_pk", 1, 0) // 0 = RESUME
        assertTrue(fileControlCalled)

        var fileChunkCalled = false
        listener.fileRecvChunkHandler = { publicKey, fileNo, position, data ->
            assertEquals("friend_pk", publicKey)
            assertEquals(1, fileNo)
            assertEquals(256L, position)
            assertTrue(byteArrayOf(9, 10).contentEquals(data))
            fileChunkCalled = true
        }
        dispatcher.onFileRecvChunk("friend_pk", 1, 256L, byteArrayOf(9, 10))
        assertTrue(fileChunkCalled)

        var chunkRequestCalled = false
        listener.fileChunkRequestHandler = { publicKey, fileNo, position, length ->
            assertEquals("friend_pk", publicKey)
            assertEquals(1, fileNo)
            assertEquals(512L, position)
            assertEquals(64, length)
            chunkRequestCalled = true
        }
        dispatcher.onFileChunkRequest("friend_pk", 1, 512L, 64)
        assertTrue(chunkRequestCalled)
    }
}
