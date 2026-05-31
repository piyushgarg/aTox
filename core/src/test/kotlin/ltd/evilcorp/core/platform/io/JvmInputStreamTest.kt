package ltd.evilcorp.core.platform.io

import java.io.ByteArrayInputStream
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmInputStreamTest {

    private lateinit var mockInputStream: ByteArrayInputStream
    private lateinit var jvmInputStream: JvmInputStream

    @BeforeTest
    fun setUp() {
        mockInputStream = ByteArrayInputStream(byteArrayOf(10, 20, 30, 40, 50))
        jvmInputStream = JvmInputStream(mockInputStream)
    }

    @Test
    fun testRead_readsBytesCorrectly() {
        val buffer = ByteArray(3)
        val bytesRead = jvmInputStream.read(buffer, 0, 3)

        assertEquals(3, bytesRead)
        assertEquals(10, buffer[0])
        assertEquals(20, buffer[1])
        assertEquals(30, buffer[2])
    }

    @Test
    fun testRead_eofReturnsNegativeOne() {
        val buffer = ByteArray(5)
        jvmInputStream.read(buffer, 0, 5)

        val nextRead = jvmInputStream.read(buffer, 0, 1)
        assertEquals(-1, nextRead)
    }

    @Test
    fun testClose_closesDelegate() {
        var closed = false
        val customInputStream = object : java.io.InputStream() {
            override fun read(): Int = -1
            override fun close() {
                closed = true
            }
        }
        val jvmStream = JvmInputStream(customInputStream)
        jvmStream.close()
        assertTrue(closed)
    }
}
