// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.backup.usecase

import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider
import javax.inject.Inject

/**
 * Use case to export selectively selected app database profiles and backup files
 * into a single encrypted or raw byte array zip archive.
 */
private const val MANIFEST_ENTRY = "manifest.txt"

open class ExportBackupUseCase @Inject constructor(
    private val providers: List<@JvmSuppressWildcards IBackupDataProvider>
) {
    open suspend fun execute(selectedIds: Set<String>): ByteArray {
        val bos = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(bos).use { zip ->
            val manifestEntry = java.util.zip.ZipEntry(MANIFEST_ENTRY)
            zip.putNextEntry(manifestEntry)
            zip.write("aTox selective backup\n".encodeToByteArray())
            zip.closeEntry()

            providers
                .filter { it.id in selectedIds }
                .forEach { provider ->
                    val entry = java.util.zip.ZipEntry("${provider.id}.bin")
                    zip.putNextEntry(entry)
                    val nonClosing = object : java.io.FilterOutputStream(zip) {
                        override fun close() {
                            flush()
                        }
                    }
                    provider.serialize(nonClosing)
                    zip.closeEntry()
                }
        }
        return bos.toByteArray()
    }
}
