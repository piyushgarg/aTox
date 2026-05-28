// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.repository

import android.content.Context
import java.io.File
import javax.inject.Inject
import ltd.evilcorp.core.db.Database
import ltd.evilcorp.domain.core.network.save.ISaveManager
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.IToxStarter
import ltd.evilcorp.domain.features.auth.repository.IProfileRepository

@Suppress("PrintStackTrace")
class ProfileRepositoryImpl @Inject constructor(
    private val context: Context,
    private val toxStarter: IToxStarter,
    private val saveManager: ISaveManager,
    private val database: Database,
) : IProfileRepository {
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

    override suspend fun clearDatabase() {
        database.clearAllTables()
    }

    private fun getDbFiles(ctx: Context): List<File> {
        val dbFile = ctx.getDatabasePath("core_db")
        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")
        return listOf(dbFile, walFile, shmFile)
    }

    private fun getCheckpointDir(ctx: Context): File {
        return File(ctx.cacheDir, "database_checkpoint")
    }

    override suspend fun createCheckpoint(): Boolean {
        return try {
            database.close()
            val checkpointDir = getCheckpointDir(context)
            if (checkpointDir.exists()) {
                checkpointDir.deleteRecursively()
            }
            checkpointDir.mkdirs()
            getDbFiles(context).forEach { file ->
                if (file.exists()) {
                    val destFile = File(checkpointDir, file.name)
                    file.copyTo(destFile, overwrite = true)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun restoreFromCheckpoint(): Boolean {
        return try {
            database.close()
            val checkpointDir = getCheckpointDir(context)
            if (!checkpointDir.exists()) return false
            getDbFiles(context).forEach { file ->
                val srcFile = File(checkpointDir, file.name)
                if (srcFile.exists()) {
                    srcFile.copyTo(file, overwrite = true)
                } else if (file.exists()) {
                    file.delete()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun clearCheckpoint() {
        try {
            val checkpointDir = getCheckpointDir(context)
            if (checkpointDir.exists()) {
                checkpointDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
