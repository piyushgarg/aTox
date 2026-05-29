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
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.contacts.usecase.GetFriendRequestsUseCase
import ltd.evilcorp.domain.features.contacts.usecase.AcceptFriendRequestUseCase
import ltd.evilcorp.domain.features.contacts.usecase.RejectFriendRequestUseCase

import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class FriendRequestsViewModel @Inject constructor(
    private val getFriendRequestsUseCase: GetFriendRequestsUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val rejectFriendRequestUseCase: RejectFriendRequestUseCase,
) : ViewModel() {
    val friendRequests: StateFlow<List<FriendRequest>> = getFriendRequestsUseCase.execute()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun acceptFriendRequest(friendRequest: FriendRequest) {
        acceptFriendRequestUseCase.execute(friendRequest)
    }

    fun rejectFriendRequest(friendRequest: FriendRequest) {
        rejectFriendRequestUseCase.execute(friendRequest)
    }
}
