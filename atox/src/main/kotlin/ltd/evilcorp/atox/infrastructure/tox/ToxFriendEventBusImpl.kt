// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.infrastructure.tox

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ltd.evilcorp.domain.features.contacts.IToxFriendEventBus
import ltd.evilcorp.domain.features.contacts.model.ToxFriendEvent

@Singleton
class ToxFriendEventBusImpl @Inject constructor() : IToxFriendEventBus {
    private val _events = MutableSharedFlow<ToxFriendEvent>(extraBufferCapacity = 128)
    override val events: SharedFlow<ToxFriendEvent> = _events.asSharedFlow()

    override fun tryEmit(event: ToxFriendEvent): Boolean {
        return _events.tryEmit(event)
    }
}
