// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.auth.usecase

import ltd.evilcorp.domain.features.auth.UserManager
import ltd.evilcorp.domain.core.model.PublicKey
import javax.inject.Inject

/**
 * Use case to verify that a profile record exists in the local database for a given public key, creating it if missing.
 */
class VerifyProfileExistsUseCase @Inject constructor(
    private val userManager: UserManager,
) {
    suspend fun execute(publicKey: PublicKey) {
        userManager.verifyExists(publicKey)
    }
}
