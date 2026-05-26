// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.domain.tox.ITox

class DeleteProfileUseCase @Inject constructor(
    private val tox: ITox,
    private val profileDeleter: IProfileDeleter,
) {
    suspend fun execute() = withContext(Dispatchers.IO) {
        val pk = tox.publicKey
        profileDeleter.deleteProfile(pk)
    }
}
