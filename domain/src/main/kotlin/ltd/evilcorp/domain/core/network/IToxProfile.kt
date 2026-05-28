// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network

import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.contacts.model.UserStatus

@Suppress("ComplexInterface")
interface IToxProfile {
    val toxId: ToxID
    val publicKey: PublicKey
    var nospam: Int

    fun getName(): String
    fun setName(name: String)
    fun getStatusMessage(): String
    fun setStatusMessage(statusMessage: String)
    fun getStatus(): UserStatus
    fun setStatus(status: UserStatus)
    fun getContacts(): List<Pair<PublicKey, Int>>
    fun acceptFriendRequest(publicKey: PublicKey)
    fun addContact(toxId: ToxID, message: String)
    fun deleteContact(publicKey: PublicKey)
    fun getFriendNumber(publicKey: PublicKey): Int
    fun getFriendPublicKey(friendNumber: Int): PublicKey?
    fun friendGetLastOnline(publicKey: PublicKey): Long
}
