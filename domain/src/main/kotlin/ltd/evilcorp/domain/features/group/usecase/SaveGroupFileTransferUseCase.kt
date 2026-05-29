package ltd.evilcorp.domain.features.group.usecase

import kotlinx.coroutines.flow.take
import ltd.evilcorp.domain.features.transfer.IFileTransferPlatformHelper
import ltd.evilcorp.domain.features.transfer.repository.IFileTransferRepository
import javax.inject.Inject

/**
 * Use case to save a completed file transfer destination path bytes to a target URI.
 */
class SaveGroupFileTransferUseCase @Inject constructor(
    private val platformHelper: IFileTransferPlatformHelper,
    private val fileTransferRepository: IFileTransferRepository,
) {
    suspend fun execute(id: Int, targetUriString: String) {
        fileTransferRepository.get(id).take(1).collect { ft ->
            val sourceUriString = ft.destination
            val path = if (sourceUriString.startsWith("file://")) {
                sourceUriString.removePrefix("file://")
            } else if (sourceUriString.startsWith("file:")) {
                sourceUriString.removePrefix("file:")
            } else {
                sourceUriString
            }
            platformHelper.saveFileToUri(path, targetUriString)
        }
    }
}
