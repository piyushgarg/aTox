package ltd.evilcorp.domain.features.backup.usecase

import ltd.evilcorp.domain.features.backup.repository.ICloudBackupRepository
import javax.inject.Inject

class DownloadCloudBackupUseCase @Inject constructor(
    private val cloudBackupRepository: ICloudBackupRepository
) {
    suspend fun execute(fileId: String): ByteArray {
        return cloudBackupRepository.downloadBackup(fileId)
    }
}
