// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.contacts.usecase

import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.contacts.FriendRequestManager
import javax.inject.Inject

/**
 * Use case to reject and discard an incoming friend request.
 */
class RejectFriendRequestUseCase @Inject constructor(
    private val friendRequestManager: FriendRequestManager,
) {
    fun execute(friendRequest: FriendRequest) {
        friendRequestManager.reject(friendRequest)
    }
}
