// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

import ltd.evilcorp.domain.feature.ISettingsFileProcessor

class AndroidSettingsFileProcessor @Inject constructor(
    private val context: Context,
    private val resolver: ContentResolver
) : ISettingsFileProcessor {
    override suspend fun readBytes(uriString: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            resolver.openInputStream(Uri.parse(uriString))?.use { it.readBytes() }
        }.getOrNull()
    }

    override suspend fun writeBytes(uriString: String, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            resolver.openOutputStream(Uri.parse(uriString))?.use { os ->
                os.write(bytes)
            } != null
        }.getOrElse { false }
    }

    override suspend fun saveUserNodesJson(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val out = File(context.filesDir, "user_nodes.json")
            out.delete()
            if (!out.createNewFile()) return@withContext false
            out.outputStream().use { it.write(bytes) }
            true
        }.getOrElse { false }
    }
}
