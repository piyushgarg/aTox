// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.auth.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.domain.core.network.IToxProfile
import ltd.evilcorp.domain.features.auth.repository.IProfileRepository

class DeleteProfileUseCase @Inject constructor(
    private val tox: IToxProfile,
    private val profileDeleter: IProfileRepository,
) {
    suspend fun execute() = withContext(Dispatchers.IO) {
        val pk = tox.publicKey
        profileDeleter.deleteProfile(pk)
    }
}
