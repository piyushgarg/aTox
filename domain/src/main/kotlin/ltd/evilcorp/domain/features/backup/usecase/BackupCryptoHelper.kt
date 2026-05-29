// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.backup.usecase

import ltd.evilcorp.domain.core.platform.IPlatformServices
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal const val MANIFEST_ENTRY = "manifest.txt"
internal const val ENCRYPTION_MAGIC = "ATOXBAK1"
private const val AES_KEY_BITS = 256
private const val GCM_TAG_BITS = 128
private const val PBKDF2_ITERATIONS = 120_000
private const val SALT_SIZE = 16
private const val IV_SIZE = 12

internal object BackupCryptoHelper {

    fun decryptIfNeeded(data: ByteArray, password: String?): ByteArray =
        if (data.startsWith(ENCRYPTION_MAGIC.encodeToByteArray())) {
            decrypt(data, password.orEmpty())
        } else {
            data
        }

    fun encrypt(data: ByteArray, password: String, platformServices: IPlatformServices): ByteArray {
        val salt = platformServices.generateSecureBytes(SALT_SIZE)
        val iv = platformServices.generateSecureBytes(IV_SIZE)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        val encryptedData = cipher.doFinal(data)
        val magicBytes = ENCRYPTION_MAGIC.encodeToByteArray()
        return magicBytes + salt + iv + encryptedData
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

internal fun ByteArray.startsWith(prefix: ByteArray): Boolean =
    size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
