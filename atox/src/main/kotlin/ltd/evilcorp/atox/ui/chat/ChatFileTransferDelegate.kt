// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat

import android.net.Uri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.core.network.INotificationHelper
import ltd.evilcorp.domain.features.transfer.deleteAll
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.backup.usecase.ExportChatHistoryUseCase
import ltd.evilcorp.domain.features.transfer.usecase.ManageFileTransferUseCase
import javax.inject.Inject

class ChatFileTransferDelegate @Inject constructor(
    private val fileTransferManager: FileTransferManager,
    private val manageFileTransferUseCase: ManageFileTransferUseCase,
    private val fileExporter: FileExporter,
    private val exportChatHistoryUseCase: ExportChatHistoryUseCase,
    private val notificationHelper: INotificationHelper,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun transfersFor(activePublicKey: Flow<PublicKey?>): Flow<List<FileTransfer>> {
        return activePublicKey.flatMapLatest { pk ->
            if (pk == null || pk.string().isEmpty()) {
                flowOf(emptyList())
            } else {
                fileTransferManager.transfersFor(pk)
            }
        }
    }

    suspend fun acceptFt(id: Int) {
        manageFileTransferUseCase.execute(ltd.evilcorp.domain.features.transfer.usecase.FileTransferAction.Accept(id))
    }

    suspend fun rejectFt(id: Int) {
        manageFileTransferUseCase.execute(ltd.evilcorp.domain.features.transfer.usecase.FileTransferAction.Reject(id))
    }

    suspend fun createFt(publicKey: PublicKey, file: Uri) {
        notificationHelper.invalidateAvatar(file.toString())
        manageFileTransferUseCase.execute(ltd.evilcorp.domain.features.transfer.usecase.FileTransferAction.Create(publicKey, file.toString()))
    }

    suspend fun deleteFt(correlationId: Int) {
        manageFileTransferUseCase.execute(ltd.evilcorp.domain.features.transfer.usecase.FileTransferAction.Delete(correlationId))
    }

    suspend fun exportFt(id: Int, dest: Uri): Result<Unit> {
        val ft = fileTransferManager.get(id).take(1).first()
        return fileExporter.exportFile(ft.destination, dest.toString())
    }

    suspend fun exportHistory(publicKey: PublicKey, dest: Uri): Result<Unit> {
        val historyContent = exportChatHistoryUseCase.execute(publicKey.string())
        return fileExporter.exportHistory(historyContent, dest.toString())
    }

    suspend fun clearTransfers(publicKey: PublicKey) {
        fileTransferManager.deleteAll(publicKey)
    }
}
