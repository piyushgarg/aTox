package ltd.evilcorp.domain.core.network.save

import ltd.evilcorp.domain.core.model.PublicKey

/**
 * Interface for the Tox profile save manager.
 * Responsible for serialization and deserialization of binary state files.
 */
interface ISaveManager {
    /**
     * Returns a list of names of all saved profiles on disk.
     */
    fun list(): List<String>

    /**
     * Performs atomic write of the binary profile state to disk.
     * @param pk The public key of the profile, acting as a unique identifier.
     * @param saveData Byte array of the Tox Core state.
     */
    fun save(pk: PublicKey, saveData: ByteArray)

    /**
     * Loads the binary profile state from disk.
     * @param pk Public key of the profile.
     * @return Byte array of the Tox Core state, or null if the profile is not found.
     */
    fun load(pk: PublicKey): ByteArray?

    /**
     * Deletes the profile save file from disk.
     * @param pk Public key of the profile.
     * @return true if the file was successfully deleted, false otherwise.
     */
    fun delete(pk: PublicKey): Boolean
}
