package ltd.evilcorp.domain.features.backup.repository

import ltd.evilcorp.domain.features.backup.model.CloudBackupInfo

interface ICloudBackupRepository {
    suspend fun listBackups(): List<CloudBackupInfo>
    suspend fun downloadBackup(fileId: String): ByteArray
}
