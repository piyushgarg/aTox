// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.repository

import javax.inject.Inject
import ltd.evilcorp.core.db.Database
import ltd.evilcorp.core.tox.save.SaveManager
import ltd.evilcorp.domain.model.PublicKey
import ltd.evilcorp.domain.tox.IToxStarter
import ltd.evilcorp.domain.usecase.IProfileDeleter

class ProfileDeleterImpl @Inject constructor(
    private val toxStarter: IToxStarter,
    private val saveManager: SaveManager,
    private val database: Database,
) : IProfileDeleter {
    override suspend fun deleteProfile(publicKey: PublicKey) {
        toxStarter.stopTox()
        saveManager.delete(publicKey)
        saveManager.list().forEach {
            try {
                saveManager.delete(PublicKey(it))
            } catch (e: Exception) {
                // Ignore
            }
        }
        database.clearAllTables()
    }
}
