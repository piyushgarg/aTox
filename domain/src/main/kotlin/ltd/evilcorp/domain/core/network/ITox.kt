// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network

interface ITox : IToxProfile, IToxMessenger, IToxFileTransmitter, IToxCallController, IToxGroupManager {
    var started: Boolean
    var isBootstrapNeeded: Boolean
    val password: String?
    val sessionId: String?

    fun changePassword(new: String?)
    fun stop()
    fun getSaveData(): ByteArray
}
