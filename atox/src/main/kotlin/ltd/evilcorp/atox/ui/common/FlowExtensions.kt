// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact

@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<Contact?>.debounceOffline(
    delayMillis: Long = 2000L
): Flow<Contact?> = transformLatest { contact ->
    if (contact == null || contact.connectionStatus != ConnectionStatus.None) {
        emit(contact)
    } else {
        delay(delayMillis)
        emit(contact)
    }
}
