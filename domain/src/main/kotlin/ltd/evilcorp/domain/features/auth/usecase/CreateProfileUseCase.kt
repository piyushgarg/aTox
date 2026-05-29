// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.auth.usecase

import ltd.evilcorp.domain.features.auth.UserManager
import ltd.evilcorp.domain.features.auth.model.User
import javax.inject.Inject

/**
 * Use case to create a new user profile record in the database.
 */
class CreateProfileUseCase @Inject constructor(
    private val userManager: UserManager,
) {
    suspend fun execute(user: User) {
        userManager.create(user)
    }
}
