// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.contacts

import kotlinx.coroutines.flow.SharedFlow
import ltd.evilcorp.domain.features.contacts.model.ToxFriendEvent

interface IToxFriendEventBus {
    val events: SharedFlow<ToxFriendEvent>
    fun tryEmit(event: ToxFriendEvent): Boolean
}
