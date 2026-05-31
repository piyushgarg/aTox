package ltd.evilcorp.core.tox

import androidx.test.ext.junit.runners.AndroidJUnit4
import ltd.evilcorp.core.tox.runtime.ToxSessionSaver
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.save.ISaveManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ToxSessionSaverTest {

    private class FakeSaveManager : ISaveManager {
        var savedPk: PublicKey? = null
        var savedData: ByteArray? = null
        override fun list(): List<String> = listOf()
        override fun load(pk: PublicKey): ByteArray? = null
        override fun save(pk: PublicKey, saveData: ByteArray) {
            savedPk = pk
            savedData = saveData
        }
        override fun delete(pk: PublicKey): Boolean = true
    }

    private lateinit var saveManager: FakeSaveManager
    private lateinit var sessionSaver: ToxSessionSaver

    @Before
    fun setUp() {
        saveManager = FakeSaveManager()
        sessionSaver = ToxSessionSaver(saveManager)
    }

    @Test
    fun testEncryptAndSave_noPassword_savesPlainBytes() {
        val pk = PublicKey("1234567890ABCDEF")
        val data = byteArrayOf(10, 20, 30, 40)

        sessionSaver.encryptAndSave(pk, data, null)

        assertEquals(pk, saveManager.savedPk)
        assertTrue(data.contentEquals(saveManager.savedData))
    }

    @Test
    fun testPasskeyDerivation_and_encryption_decryption() {
        val password = "mySecretPassword"
        val salt = ByteArray(32) { it.toByte() }

        // 1. Derive passkey
        val passkey = sessionSaver.derivePasskey(password, salt)
        assertNotNull(passkey)
        assertTrue(passkey.isNotEmpty())

        // 2. Encrypt some data
        val pk = PublicKey("ABCDEF1234567890")
        val originalData = "ToxSaveDataPlaintextBytes".toByteArray()

        sessionSaver.encryptAndSave(pk, originalData, passkey)

        val encryptedData = saveManager.savedData
        assertNotNull(encryptedData)
        // Encrypted data should be different from original plaintext
        assertTrue(!originalData.contentEquals(encryptedData))

        // 3. Decrypt
        val decryptedData = sessionSaver.decrypt(encryptedData, passkey)
        assertNotNull(decryptedData)
        assertTrue(originalData.contentEquals(decryptedData))
    }
}
