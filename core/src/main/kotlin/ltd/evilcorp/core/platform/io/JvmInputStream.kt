package ltd.evilcorp.core.platform.io

import ltd.evilcorp.domain.core.io.IInputStream
import java.io.InputStream

class JvmInputStream(private val delegate: InputStream) : IInputStream {
    override fun read(bytes: ByteArray, offset: Int, length: Int): Int = delegate.read(bytes, offset, length)
    override fun close() = delegate.close()
}
