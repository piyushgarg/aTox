package ltd.evilcorp.domain.features.backup.repository

interface IBackupDataProvider {
    val id: String
    val displayNameRes: Int
    val descriptionRes: Int
    suspend fun serialize(outputStream: java.io.OutputStream)
    suspend fun deserialize(data: ByteArray)
}
