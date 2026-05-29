// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.settings.usecase

import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.features.transfer.getCacheSize
import javax.inject.Inject

/**
 * Use case to query the total size of the file transfer cache storage on disk.
 */
class GetCacheSizeUseCase @Inject constructor(
    private val fileTransferManager: FileTransferManager,
) {
    fun execute(): Long {
        return fileTransferManager.getCacheSize()
    }
}
