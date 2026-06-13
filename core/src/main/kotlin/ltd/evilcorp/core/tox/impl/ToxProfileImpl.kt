// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox.impl

import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.core.tox.runtime.ToxRuntime
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.IToxProfile
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.features.contacts.model.UserStatus

@Singleton
class ToxProfileImpl @Inject constructor(
    private val runtime: ToxRuntime,
) : IToxProfile {
    override val toxId: ToxID get() = runtime.toxId
    override val publicKey: PublicKey get() = runtime.publicKey
    override var nospam: Int
        get() = runtime.nospam
        set(value) {
            runtime.nospam = value
        }

    override fun getName(): String = runtime.getName()
    override fun setName(name: String) {
        runtime.setName(name)
    }

    override fun getStatusMessage(): String = runtime.getStatusMessage()
    override fun setStatusMessage(statusMessage: String) {
        runtime.setStatusMessage(statusMessage)
    }

    override fun getStatus(): UserStatus = runtime.getStatus()
    override fun setStatus(status: UserStatus) {
        runtime.setStatus(status)
    }

    override fun getContacts(): List<Pair<PublicKey, Int>> = runtime.getContacts()

    override fun acceptFriendRequest(publicKey: PublicKey): Result<Unit> =
        runtime.acceptFriendRequest(publicKey)

    override fun addFriendNoRequest(publicKey: PublicKey): Int =
        runtime.addFriendNoRequest(publicKey)

    override fun addContact(toxId: ToxID, message: String) {
        runtime.addContact(toxId, message)
    }

    override fun deleteContact(publicKey: PublicKey) {
        runtime.deleteContact(publicKey)
    }

    override fun getFriendNumber(publicKey: PublicKey): Int = runtime.getFriendNumber(publicKey)

    override fun getFriendPublicKey(friendNumber: Int): PublicKey? {
        val bytes = runtime.getFriendPublicKey(friendNumber) ?: return null
        return PublicKey.fromBytes(bytes)
    }

    override fun friendGetLastOnline(publicKey: PublicKey): Long = runtime.friendGetLastOnline(publicKey)
}
