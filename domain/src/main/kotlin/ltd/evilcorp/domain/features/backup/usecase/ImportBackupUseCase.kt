// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.backup.usecase

import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider
import ltd.evilcorp.domain.core.platform.IPlatformServices
import javax.inject.Inject

/**
 * Use case to import, decrypt, and parse selected app backup profiles, restoring database states.
 */
open class ImportBackupUseCase @Inject constructor(
    private val providers: List<@JvmSuppressWildcards IBackupDataProvider>,
    private val platformServices: IPlatformServices
) {
    open suspend fun execute(data: ByteArray, password: String? = null, skipIds: Set<String> = emptySet()) {
        val zipBytes = BackupCryptoHelper.decryptIfNeeded(data, password)
        val providerById = providers.associateBy { it.id }
        val files = platformServices.unzip(zipBytes)

        files.forEach { (name, content) ->
            val id = name.removeSuffix(".bin")
            if (id in skipIds) return@forEach
            val provider = providerById[id] ?: return@forEach
            provider.deserialize(content)
        }
    }
}
