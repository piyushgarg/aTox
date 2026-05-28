// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.auth.repository

import ltd.evilcorp.domain.core.model.PublicKey

interface IProfileRepository {
    suspend fun deleteProfile(publicKey: PublicKey)
    suspend fun clearDatabase()
    suspend fun createCheckpoint(): Boolean
    suspend fun restoreFromCheckpoint(): Boolean
    suspend fun clearCheckpoint()
}
