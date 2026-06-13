package ltd.evilcorp.domain.features.backup.model

data class CloudBackupInfo(
    val id: String,
    val name: String,
    val sizeKb: Long,
    val createdTimeMs: Long
)
