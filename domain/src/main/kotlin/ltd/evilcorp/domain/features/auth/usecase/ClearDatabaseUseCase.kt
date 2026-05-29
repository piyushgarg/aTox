// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.auth.usecase

import ltd.evilcorp.domain.features.auth.repository.IProfileRepository
import javax.inject.Inject

/**
 * Use case to completely clear the local SQLite/Room storage database.
 */
class ClearDatabaseUseCase @Inject constructor(
    private val profileRepository: IProfileRepository,
) {
    suspend fun execute() {
        profileRepository.clearDatabase()
    }
}
