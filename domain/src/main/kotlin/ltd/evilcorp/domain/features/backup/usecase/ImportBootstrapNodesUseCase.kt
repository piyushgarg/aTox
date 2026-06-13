// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.backup.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.domain.core.di.IoDispatcher
import ltd.evilcorp.domain.features.settings.ISettingsFileProcessor

/**
 * Use case to import and save user-customized Tox bootstrap nodes JSON files.
 */
class ImportBootstrapNodesUseCase @Inject constructor(
    private val fileProcessor: ISettingsFileProcessor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun execute(uriString: String): Boolean = withContext(ioDispatcher) {
        val bytes = fileProcessor.readBytes(uriString) ?: return@withContext false
        return@withContext fileProcessor.saveUserNodesJson(bytes)
    }
}

