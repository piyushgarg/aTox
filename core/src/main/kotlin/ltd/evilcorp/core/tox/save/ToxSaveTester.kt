package ltd.evilcorp.core.tox.save

import ltd.evilcorp.core.tox.NativeTox
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus

/**
 * Performs a test load of Tox binary data with decryption (if a password is provided).
 * Verifies the correctness of the data structure before initializing the main active instance.
 * @param options Save options containing the binary buffer saveData.
 * @param password Optional password for profile decryption.
 * @return Validation status as a [ToxSaveStatus].
 */
fun testToxSave(options: SaveOptions, password: String?): ToxSaveStatus {
    val native = NativeTox()
    return try {
        val rawSaveData = options.saveData
        val saveData = if (password != null && rawSaveData != null) {
            val salt = native.getSalt(rawSaveData) ?: return ToxSaveStatus.BadFormat
            val passkey = native.passKeyDeriveWithSalt(password.toByteArray(), salt)
                ?: return ToxSaveStatus.OutOfMemory
            native.passDecrypt(rawSaveData, passkey) ?: return ToxSaveStatus.Encrypted
        } else {
            rawSaveData
        }
        val toxPtr = native.toxNew(saveData)
        if (toxPtr == 0L) {
            return ToxSaveStatus.BadFormat
        }
        native.toxKill(toxPtr)
        ToxSaveStatus.Ok
    } catch (e: Exception) {
        ToxSaveStatus.BadFormat
    }
}
