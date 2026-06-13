package ltd.evilcorp.atox.infrastructure.tox

import kotlinx.serialization.Serializable

internal const val TYPE_SUMMARY = "group_sync_summary"
internal const val TYPE_IDS_PAGE = "group_sync_ids_page"
internal const val TYPE_MSG_PAGE = "group_sync_msg_page"
internal const val TYPE_GROUP_RELAY = "group_relay_status"

// Lossless Tox packet limit (~1373 bytes).
// Pack IDs within ~900 bytes, messages within ~900 bytes with item size checks.
internal const val MAX_IDS_PER_PAGE = 80
internal const val MSG_BYTE_BUDGET = 900

// Delay between ID exchange pages to avoid overwhelming the lossless channel.
internal const val PAGE_DELAY_MS = 200L

@Serializable
data class SyncMessagePayload(
    val peerId: Int,
    val senderName: String,
    val message: String,
    val timestamp: Long,
    val correlationId: Int,
    val type: String
)

@Serializable
data class GroupRelayEntry(
    val chatId: String,
    val status: String, // "connected", "connecting", "disconnected"
    val groupNumber: Int = -1
)

@Serializable
data class GroupSyncPayload(
    val type: String,
    val chatId: String = "",
    val count: Int = -1,
    val lastId: Int = -1,
    val page: Int = 0,
    val more: Boolean = false,
    val ids: List<Int> = emptyList(),
    val messages: List<SyncMessagePayload> = emptyList(),
    val groups: List<GroupRelayEntry> = emptyList()
)
