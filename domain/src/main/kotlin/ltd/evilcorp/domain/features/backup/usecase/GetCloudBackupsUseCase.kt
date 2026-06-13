package ltd.evilcorp.domain.features.backup.usecase

import ltd.evilcorp.domain.features.backup.model.CloudBackupInfo
import ltd.evilcorp.domain.features.backup.repository.ICloudBackupRepository
import javax.inject.Inject

class GetCloudBackupsUseCase @Inject constructor(
    private val cloudBackupRepository: ICloudBackupRepository
) {
    suspend fun execute(): List<CloudBackupInfo> {
        return cloudBackupRepository.listBackups()
    }
}
