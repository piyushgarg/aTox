// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.transfer.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.core.model.PublicKey
import javax.inject.Inject

sealed interface FileTransferAction {
    data class Accept(val fileNumber: Int) : FileTransferAction
    data class Reject(val fileNumber: Int) : FileTransferAction
    data class Delete(val correlationId: Int) : FileTransferAction
    data class Create(val publicKey: PublicKey, val fileUriString: String) : FileTransferAction
}

class ManageFileTransferUseCase @Inject constructor(
    private val fileTransferManager: FileTransferManager
) {
    suspend fun execute(action: FileTransferAction) = withContext(Dispatchers.IO) {
        when (action) {
            is FileTransferAction.Accept -> fileTransferManager.accept(action.fileNumber)
            is FileTransferAction.Reject -> fileTransferManager.reject(action.fileNumber)
            is FileTransferAction.Delete -> fileTransferManager.delete(action.correlationId)
            is FileTransferAction.Create -> fileTransferManager.create(action.publicKey, action.fileUriString)
        }
    }
}
