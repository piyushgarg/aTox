// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network

interface IFileStorageProvider {
    fun exists(uriString: String): Boolean
    fun lastModified(uriString: String): Long
    fun size(uriString: String): Long
    fun getAbsolutePath(uriString: String): String?
}
