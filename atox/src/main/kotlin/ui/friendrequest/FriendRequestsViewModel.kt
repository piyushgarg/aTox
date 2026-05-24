// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.friendrequest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import ltd.evilcorp.core.model.FriendRequest
import ltd.evilcorp.domain.feature.FriendRequestManager

class FriendRequestsViewModel @Inject constructor(
    private val friendRequestManager: FriendRequestManager,
) : ViewModel() {
    val friendRequests: StateFlow<List<FriendRequest>> = friendRequestManager.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun acceptFriendRequest(friendRequest: FriendRequest) {
        friendRequestManager.accept(friendRequest)
    }

    fun rejectFriendRequest(friendRequest: FriendRequest) {
        friendRequestManager.reject(friendRequest)
    }
}
