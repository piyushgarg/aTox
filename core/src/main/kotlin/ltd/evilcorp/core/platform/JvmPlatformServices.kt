package ltd.evilcorp.core.platform

import ltd.evilcorp.domain.core.platform.IPlatformServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.security.SecureRandom
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JvmPlatformServices @Inject constructor() : IPlatformServices {
    private val secureRandom = SecureRandom()

    override fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    override fun generateSecureBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    override fun zip(files: Map<String, ByteArray>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            files.forEach { (name, content) ->
                val entry = ZipEntry(name)
                zip.putNextEntry(entry)
                zip.write(content)
                zip.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    override fun unzip(zipBytes: ByteArray): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                result[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return result
    }
}
