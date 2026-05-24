// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.chat

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

interface FileExporter {
    suspend fun exportFile(sourceUriString: String, destUriString: String): Result<Unit>
    suspend fun exportHistory(content: String, destUriString: String): Result<Unit>
}

class AndroidFileExporter @Inject constructor(
    private val resolver: ContentResolver
) : FileExporter {
    override suspend fun exportFile(sourceUriString: String, destUriString: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sourceUri = Uri.parse(sourceUriString)
                val destUri = Uri.parse(destUriString)
                resolver.openInputStream(sourceUri)?.use { ins ->
                    resolver.openOutputStream(destUri).use { os ->
                        ins.copyTo(os ?: throw IOException("Failed to open output stream"))
                    }
                } ?: throw IOException("Failed to open input stream")
                Unit
            }
        }

    override suspend fun exportHistory(content: String, destUriString: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val destUri = Uri.parse(destUriString)
                resolver.openOutputStream(destUri).use { os ->
                    content.byteInputStream().copyTo(os ?: throw IOException("Failed to open output stream"))
                }
                Unit
            }
        }
}
