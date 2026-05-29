package ltd.evilcorp.domain.core.io

interface IInputStream {
    fun read(bytes: ByteArray, offset: Int, length: Int): Int
    fun close()
}
