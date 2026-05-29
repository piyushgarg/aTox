// SPDX-FileCopyrightText: 2022 Akito <the@akito.ooo>
// SPDX-FileCopyrightText: 2023-2024 Robin Lindén <dev@robinlinden.eu>
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.features.backup

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import ltd.evilcorp.domain.core.platform.IPlatformServices
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class ExportData(
    val version: Int,
    val timestamp: String,
    @SerialName("contact_public_key")
    val contactPublicKey: String,
    val entries: List<ExportEntry>
)

@Serializable
internal data class ExportEntry(
    val message: String,
    val sender: String,
    val type: String,
    val timestamp: String
)

class ExportManager @Inject constructor(
    private val messageRepository: IMessageRepository,
    private val platformServices: IPlatformServices
) {
    private val json = Json { prettyPrint = true; prettyPrintIndent = "  " }

    fun generateExportMessagesJString(publicKey: String): String {
        val messages = runBlocking { messageRepository.get(publicKey).first() }

        val entries = messages.map {
            ExportEntry(
                message = it.message,
                sender = it.sender.toString(),
                type = it.type.toString(),
                timestamp = platformServices.formatDate(it.timestamp)
            )
        }

        val exportData = ExportData(
            version = 1,
            timestamp = platformServices.formatDate(System.currentTimeMillis()),
            contactPublicKey = publicKey,
            entries = entries
        )

        return json.encodeToString(ExportData.serializer(), exportData)
    }
}
