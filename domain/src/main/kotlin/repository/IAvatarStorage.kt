package ltd.evilcorp.domain.repository

interface IAvatarStorage {
    suspend fun saveSelfAvatar(bytes: ByteArray): Boolean
}
