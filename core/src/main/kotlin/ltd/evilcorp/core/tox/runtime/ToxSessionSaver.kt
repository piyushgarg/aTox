package ltd.evilcorp.core.tox.runtime

import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.domain.core.network.save.ISaveManager
import ltd.evilcorp.domain.core.model.PublicKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToxSessionSaver @Inject constructor(
    private val saveManager: ISaveManager
) {
    fun encryptAndSave(publicKey: PublicKey, saveData: ByteArray, passkey: ByteArray?) {
        val encryptedData = if (passkey != null) {
            NativeTox().passEncrypt(saveData, passkey) ?: saveData
        } else {
            saveData
        }
        saveManager.save(publicKey, encryptedData)
    }

    fun decrypt(saveData: ByteArray, passkey: ByteArray): ByteArray? {
        return NativeTox().passDecrypt(saveData, passkey)
    }

    fun derivePasskey(password: String, salt: ByteArray): ByteArray? {
        return NativeTox().passKeyDeriveWithSalt(password.toByteArray(), salt)
    }
}
