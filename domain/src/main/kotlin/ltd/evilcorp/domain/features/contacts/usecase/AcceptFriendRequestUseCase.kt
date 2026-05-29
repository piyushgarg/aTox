// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.contacts.usecase

import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.contacts.FriendRequestManager
import javax.inject.Inject

/**
 * Use case to accept an incoming friend request, adding the user as a contact and initializing messages.
 */
class AcceptFriendRequestUseCase @Inject constructor(
    private val friendRequestManager: FriendRequestManager,
) {
    fun execute(friendRequest: FriendRequest) {
        friendRequestManager.accept(friendRequest)
    }
}
