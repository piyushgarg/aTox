// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.platform.storage

import ltd.evilcorp.domain.core.network.IFileStorageProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidFileStorageProvider @Inject constructor() : IFileStorageProvider {
    override fun exists(uriString: String): Boolean = try {
        getFile(uriString)?.exists() ?: false
    } catch (_: Exception) {
        false
    }

    override fun lastModified(uriString: String): Long = try {
        getFile(uriString)?.lastModified() ?: 0L
    } catch (_: Exception) {
        0L
    }

    override fun size(uriString: String): Long = try {
        getFile(uriString)?.length() ?: 0L
    } catch (_: Exception) {
        0L
    }

    override fun getAbsolutePath(uriString: String): String? = getFile(uriString)?.absolutePath

    private fun getFile(uriString: String): File? {
        if (uriString.isEmpty()) return null
        return try {
            val path = if (uriString.startsWith("file://")) {
                java.net.URI(uriString).path
            } else if (uriString.startsWith("file:")) {
                uriString.substringAfter("file:").substringBefore("?")
            } else {
                uriString
            }
            File(path)
        } catch (_: Exception) {
            try {
                val cleanPath = uriString.replace("file://", "").substringBefore("?")
                File(cleanPath)
            } catch (_: Exception) {
                null
            }
        }
    }
}
