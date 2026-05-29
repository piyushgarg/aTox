// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.contacts.usecase

import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.ITox
import javax.inject.Inject

/**
 * Use case to retrieve the public key of a friend using their Tox friend number.
 */
class GetFriendPublicKeyUseCase @Inject constructor(
    private val tox: ITox,
) {
    fun execute(friendNumber: Int): PublicKey? {
        return tox.getFriendPublicKey(friendNumber)
    }
}
