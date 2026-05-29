// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.backup.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.domain.features.settings.ISettingsFileProcessor
import ltd.evilcorp.domain.core.network.bootstrap.BootstrapNodeJsonParser

/**
 * Use case to validate raw JSON bytes representing Tox bootstrap nodes.
 */
class ValidateBootstrapNodesUseCase @Inject constructor(
    private val fileProcessor: ISettingsFileProcessor,
    private val nodeParser: BootstrapNodeJsonParser,
) {
    suspend fun execute(uriString: String): Boolean = withContext(Dispatchers.IO) {
        val bytes = fileProcessor.readBytes(uriString) ?: return@withContext false
        return@withContext nodeParser.parse(bytes.decodeToString()).isNotEmpty()
    }
}
