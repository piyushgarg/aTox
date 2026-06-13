// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.backup.usecase

import ltd.evilcorp.domain.core.platform.IPlatformServices
import javax.inject.Inject

/**
 * Use case to extract a specific provider's data binary block from a backup archive without deserializing the whole.
 */
open class GetBackupProviderDataUseCase @Inject constructor(
    private val platformServices: IPlatformServices
) {
    open suspend fun execute(data: ByteArray, id: String): ByteArray? {
        val files = platformServices.unzip(data)
        return files["$id.bin"]
    }
}
