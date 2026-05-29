// SPDX-FileCopyrightText: 2020-2024 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.db

import kotlin.test.Test
import kotlin.test.assertEquals
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.contacts.model.UserStatus

class ConvertersTest {
    @Test
    fun `user status can be converted`() {
        UserStatus.entries.forEach {
            assertEquals(it.id, Converters.fromStatus(it))
            assertEquals(it, Converters.toStatus(it.id))
        }
    }

    @Test
    fun `connection status can be converted`() {
        ConnectionStatus.entries.forEach {
            assertEquals(it.id, Converters.fromConnection(it))
            assertEquals(it, Converters.toConnection(it.id))
        }
    }

    @Test
    fun `sender can be converted`() {
        Sender.entries.forEach {
            assertEquals(it.id, Converters.fromSender(it))
            assertEquals(it, Converters.toSender(it.id))
        }
    }

    @Test
    fun `message type can be converted`() {
        MessageType.entries.forEach {
            assertEquals(it.id, Converters.fromMessageType(it))
            assertEquals(it, Converters.toMessageType(it.id))
        }
    }
}
