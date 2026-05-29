// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.backup.usecase

import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider
import ltd.evilcorp.domain.core.platform.IPlatformServices
import javax.inject.Inject

/**
 * Use case to export selectively selected app database profiles and backup files
 * into a single encrypted or raw byte array zip archive.
 */
open class ExportBackupUseCase @Inject constructor(
    private val providers: List<@JvmSuppressWildcards IBackupDataProvider>,
    private val platformServices: IPlatformServices
) {
    open suspend fun execute(selectedIds: Set<String>, password: String? = null): ByteArray {
        val files = mutableMapOf<String, ByteArray>()
        files[MANIFEST_ENTRY] = "aTox selective backup\n".encodeToByteArray()

        providers
            .filter { it.id in selectedIds }
            .forEach { provider ->
                files["${provider.id}.bin"] = provider.serialize()
            }

        val zipBytes = platformServices.zip(files)

        return password?.takeIf(String::isNotBlank)?.let { BackupCryptoHelper.encrypt(zipBytes, it, platformServices) } ?: zipBytes
    }
}
