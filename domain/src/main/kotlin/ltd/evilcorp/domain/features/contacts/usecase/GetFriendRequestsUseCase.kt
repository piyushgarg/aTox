// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.contacts.usecase

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.contacts.FriendRequestManager
import javax.inject.Inject

/**
 * Use case to retrieve the active stream of incoming friend requests.
 */
class GetFriendRequestsUseCase @Inject constructor(
    private val friendRequestManager: FriendRequestManager,
) {
    fun execute(): Flow<List<FriendRequest>> {
        return friendRequestManager.getAll()
    }
}
