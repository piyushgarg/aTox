// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.settings.usecase

import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.features.transfer.clearCache
import javax.inject.Inject

/**
 * Use case to clear the file transfer temporary cache storage on disk.
 */
class ClearCacheUseCase @Inject constructor(
    private val fileTransferManager: FileTransferManager,
) {
    fun execute() {
        fileTransferManager.clearCache()
    }
}
