package ltd.evilcorp.domain.backup

interface BackupDataProvider {
    val id: String
    val displayNameRes: Int
    val descriptionRes: Int
    fun serialize(): ByteArray
    fun deserialize(data: ByteArray)
}
