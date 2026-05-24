// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.settings

interface SettingsFileProcessor {
    suspend fun readBytes(uriString: String): ByteArray?
    suspend fun writeBytes(uriString: String, bytes: ByteArray): Boolean
    suspend fun saveUserNodesJson(bytes: ByteArray): Boolean
}
