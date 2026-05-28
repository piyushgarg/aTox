package ltd.evilcorp.domain.features.backup.usecase

import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

private const val MANIFEST_ENTRY = "manifest.txt"
private const val ENCRYPTION_MAGIC = "ATOXBAK1"
private const val AES_KEY_BITS = 256
private const val GCM_TAG_BITS = 128
private const val PBKDF2_ITERATIONS = 120_000
private const val SALT_SIZE = 16
private const val IV_SIZE = 12

open class BackupUseCase @Inject constructor(
    val providers: List<@JvmSuppressWildcards IBackupDataProvider>,
) {
    open suspend fun export(selectedIds: Set<String>, password: String? = null): ByteArray {
        val zipBytes = ByteArrayOutputStream().use { bytes ->
            ZipOutputStream(bytes).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write("aTox selective backup\n".encodeToByteArray())
                zip.closeEntry()

                providers
                    .filter { it.id in selectedIds }
                    .forEach { provider ->
                        zip.putNextEntry(ZipEntry("${provider.id}.bin"))
                        zip.write(provider.serialize())
                        zip.closeEntry()
                    }
            }
            bytes.toByteArray()
        }

        return password?.takeIf(String::isNotBlank)?.let { encrypt(zipBytes, it) } ?: zipBytes
    }

    open suspend fun import(data: ByteArray, password: String? = null, skipIds: Set<String> = emptySet()) {
        val zipBytes = decryptIfNeeded(data, password)
        val providerById = providers.associateBy { it.id }

        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                val id = entry.name.removeSuffix(".bin")
                if (id in skipIds) return@forEach
                val provider = providerById[id] ?: return@forEach
                provider.deserialize(zip.readBytes())
                zip.closeEntry()
            }
        }
    }

    open suspend fun providerData(data: ByteArray, password: String? = null, id: String): ByteArray? {
        ZipInputStream(ByteArrayInputStream(decryptIfNeeded(data, password))).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                if (entry.name == "$id.bin") {
                    return zip.readBytes()
                }
                zip.closeEntry()
            }
        }
        return null
    }

    private fun decryptIfNeeded(data: ByteArray, password: String?): ByteArray =
        if (data.startsWith(ENCRYPTION_MAGIC.encodeToByteArray())) {
            decrypt(data, password.orEmpty())
        } else {
            data
        }

    private fun encrypt(data: ByteArray, password: String): ByteArray {
        val salt = SecureRandom().generateSeed(SALT_SIZE)
        val iv = SecureRandom().generateSeed(IV_SIZE)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
        }

        return ByteArrayOutputStream().use { out ->
            out.write(ENCRYPTION_MAGIC.encodeToByteArray())
            out.write(salt)
            out.write(iv)
            out.write(cipher.doFinal(data))
            out.toByteArray()
        }
    }

    private fun decrypt(data: ByteArray, password: String): ByteArray {
        require(password.isNotBlank()) { "Password is required for encrypted backup" }
        val magicSize = ENCRYPTION_MAGIC.length
        val salt = data.copyOfRange(magicSize, magicSize + SALT_SIZE)
        val ivStart = magicSize + SALT_SIZE
        val iv = data.copyOfRange(ivStart, ivStart + IV_SIZE)
        val encrypted = data.copyOfRange(ivStart + IV_SIZE, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return cipher.doFinal(encrypted)
    }

    private fun key(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_BITS)
        val encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(encoded, "AES")
    }
}

private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
    size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
