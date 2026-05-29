package ltd.evilcorp.atox.infrastructure.tox

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.group.model.GroupMessage
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.GroupManager
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "GroupSyncManager"

private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

private val SYNC_PACKET_PREFIX = byteArrayOf(0xA0.toByte())

private const val TYPE_SUMMARY = "group_sync_summary"
private const val TYPE_IDS_PAGE = "group_sync_ids_page"
private const val TYPE_MSG_PAGE = "group_sync_msg_page"

// Lossless Tox packet limit (~1373 bytes).
// Pack IDs within ~900 bytes, messages within ~900 bytes with item size checks.
private const val MAX_IDS_PER_PAGE = 80
private const val MSG_BYTE_BUDGET = 900

// Delay between ID exchange pages to avoid overwhelming the lossless channel.
private const val PAGE_DELAY_MS = 200L

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
data class GroupSyncPayload(
    val type: String,
    val chatId: String,
    val count: Int = -1,
    val lastId: Int = -1,
    val page: Int = 0,
    val more: Boolean = false,
    val ids: List<Int> = emptyList(),
    val messages: List<SyncMessagePayload> = emptyList()
)

@Singleton
class GroupSyncManager @Inject constructor(
    private val scope: CoroutineScope,
    private val groupRepository: IGroupRepository,
    private val tox: ITox,
    private val groupManager: GroupManager
) {
    // Store accumulated IDs for multi-page requests (key = chatId|fromPk).
    private val pendingIdPages = ConcurrentHashMap<String, MutableSet<Int>>()

    // ========================================================================
    // Entry Points
    // ========================================================================

    /**
     * Triggered when a friend comes online. Triggers reconnection or message synchronization.
     */
    fun onGroupPeerOnline(friendPublicKey: String) {
        scope.launch {
            val isBootstrapOnly = groupManager.isBootstrapFriend(friendPublicKey)
            val groups = findGroupsWithPeer(friendPublicKey)
            for (chatId in groups) {
                val group = groupRepository.get(chatId).firstOrNull() ?: continue
                
                // If group is disconnected or reconnecting, trigger reconnect
                val currentStatus = groupManager.connectionStatus(chatId)
                if ((currentStatus == GroupConnectionStatus.Disconnected || currentStatus == GroupConnectionStatus.Reconnecting) && group.groupNumber >= 0) {
                    tox.groupReconnect(group.groupNumber)
                }
                
                // Sync only with real chat participants, not bootstrap-only friends
                if (!isBootstrapOnly) {
                    sendSummary(chatId, friendPublicKey)
                }
            }
        }
    }

    /**
     * Handles incoming lossless custom data packets containing sync protocol payloads.
     */
    fun handleLosslessPacket(fromPublicKey: String, data: ByteArray) {
        if (data.size < 2 || data[0] != SYNC_PACKET_PREFIX[0]) return
        val text = try {
            data.copyOfRange(1, data.size).decodeToString()
        } catch (e: Exception) {
            return
        }

        scope.launch {
            try {
                val payload = json.decodeFromString<GroupSyncPayload>(text)
                when (payload.type) {
                    TYPE_SUMMARY -> onSummaryReceived(fromPublicKey, payload)
                    TYPE_IDS_PAGE -> onIdsPageReceived(fromPublicKey, payload)
                    TYPE_MSG_PAGE -> onMsgPageReceived(fromPublicKey, payload)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Parse error: $e")
            }
        }
    }

    // ========================================================================
    // Phase 1 — Summary Exchange (Each side sends: count, lastId)
    // ========================================================================

    private suspend fun sendSummary(chatId: String, peerPk: String) {
        val allIds = groupRepository.getMessageIds(chatId)
        val validIds = allIds.filter { it >= 0 } // Exclude unsent (-1)
        val lastId = validIds.maxOrNull() ?: return // Empty group, nothing to sync

        val payload = GroupSyncPayload(
            type = TYPE_SUMMARY,
            chatId = chatId,
            count = validIds.size,
            lastId = lastId
        )
        sendPacket(peerPk, json.encodeToString(payload))

        Log.i(TAG, "Summary sent for $chatId: count=${validIds.size}, lastId=$lastId")
    }

    private fun onSummaryReceived(fromPk: String, payload: GroupSyncPayload) {
        val chatId = payload.chatId
        if (chatId.isEmpty()) return
        val theirCount = payload.count
        val theirLastId = payload.lastId
        if (theirCount < 0 || theirLastId < 0) return

        scope.launch {
            val myIds = groupRepository.getMessageIds(chatId).filter { it >= 0 }
            val myCount = myIds.size
            val myLastId = myIds.maxOrNull() ?: -1

            // If summaries match, we are in-sync
            if (myCount == theirCount && myLastId == theirLastId) {
                Log.i(TAG, "In-sync for $chatId (count=$myCount, lastId=$myLastId)")
                return@launch
            }

            Log.i(TAG, "Mismatch for $chatId: local(count=$myCount, lastId=$myLastId) vs remote(count=$theirCount, lastId=$theirLastId)")

            // Reply with the first page of our message IDs
            val sorted = myIds.sorted()
            val page = sorted.take(MAX_IDS_PER_PAGE)
            val hasMore = sorted.size > MAX_IDS_PER_PAGE
            pendingIdPages["$chatId|$fromPk"] = mutableSetOf() // Reset accumulated pages

            sendIdsPage(fromPk, chatId, page, 0, hasMore)
        }
    }

    // ========================================================================
    // Phase 2 — Multi-page ID exchange
    // ========================================================================

    private suspend fun sendIdsPage(toPk: String, chatId: String, ids: List<Int>, pageNum: Int, more: Boolean) {
        val payload = GroupSyncPayload(
            type = TYPE_IDS_PAGE,
            chatId = chatId,
            page = pageNum,
            more = more,
            ids = ids
        )
        sendPacket(toPk, json.encodeToString(payload))
    }

    private fun onIdsPageReceived(fromPk: String, payload: GroupSyncPayload) {
        val chatId = payload.chatId
        if (chatId.isEmpty()) return
        val page = payload.page
        val more = payload.more
        val ids = payload.ids

        // Accumulate received IDs
        val accumulator = pendingIdPages.getOrPut("$chatId|$fromPk") { mutableSetOf() }
        accumulator.addAll(ids)

        Log.i(TAG, "Received IDs page $page for $chatId (accumulated=${accumulator.size}, more=$more)")

        if (more) {
            // Wait for subsequent pages
            return
        }

        // All ID pages received, execute comparison
        scope.launch {
            val remoteIds = accumulator.toList()
            pendingIdPages.remove("$chatId|$fromPk")

            val myIds = groupRepository.getMessageIds(chatId).filter { it >= 0 }

            // Find missing messages we have, but they don't, and send them
            val remoteSet = remoteIds.toSet()
            val theyNeed = myIds.filter { it !in remoteSet }
            if (theyNeed.isEmpty()) {
                Log.i(TAG, "No messages to send for $chatId")
                return@launch
            }

            Log.i(TAG, "Sending ${theyNeed.size} missing messages for $chatId to $fromPk")
            sendMsgPages(fromPk, chatId, theyNeed.sorted())
        }
    }

    // ========================================================================
    // Phase 3 — Send messages in size-controlled batches
    // ========================================================================

    private suspend fun sendMsgPages(toPk: String, chatId: String, msgIds: List<Int>) {
        var index = 0
        val total = msgIds.size
        while (index < total) {
            val batchMsgs = mutableListOf<SyncMessagePayload>()
            var byteEstimate = 0

            while (index < total) {
                val id = msgIds[index]
                val msgs = groupRepository.getMessagesByIds(chatId, setOf(id))
                if (msgs.isNotEmpty()) {
                    val msg = msgs.first()
                    val obj = SyncMessagePayload(
                        peerId = msg.peerId,
                        senderName = msg.senderName,
                        message = msg.message,
                        timestamp = msg.timestamp,
                        correlationId = msg.correlationId,
                        type = msg.type.name
                    )

                    val cost = json.encodeToString(obj).toByteArray().size + 1
                    if (batchMsgs.isNotEmpty() && byteEstimate + cost > MSG_BYTE_BUDGET) {
                        break
                    }

                    batchMsgs.add(obj)
                    byteEstimate += cost
                }
                index++
            }

            val more = index < total
            val payload = GroupSyncPayload(
                type = TYPE_MSG_PAGE,
                chatId = chatId,
                more = more,
                messages = batchMsgs
            )

            sendPacket(toPk, json.encodeToString(payload))
            if (more) delay(PAGE_DELAY_MS)
        }
    }

    private suspend fun onMsgPageReceived(fromPk: String, payload: GroupSyncPayload) {
        val chatId = payload.chatId
        if (chatId.isEmpty()) return
        val more = payload.more
        val messages = payload.messages

        var saved = 0
        for (obj in messages) {
            val corrId = obj.correlationId
            if (groupRepository.existsByCorrelationId(chatId, corrId)) continue

            val msg = GroupMessage(
                groupChatId = chatId,
                peerId = obj.peerId,
                senderName = obj.senderName,
                message = obj.message,
                sender = Sender.Received,
                type = try { MessageType.valueOf(obj.type) }
                        catch (e: Exception) { MessageType.Normal },
                correlationId = corrId,
                timestamp = obj.timestamp,
            )
            groupRepository.addMessage(msg)
            saved++
        }
        Log.i(TAG, "Saved $saved synced messages for $chatId from $fromPk (more=$more)")
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private suspend fun findGroupsWithPeer(peerPublicKey: String): List<String> {
        val allGroups = groupRepository.getAll().firstOrNull() ?: return emptyList()
        val result = mutableListOf<String>()
        for (group in allGroups) {
            val peers = groupRepository.getPeers(group.chatId).firstOrNull() ?: continue
            if (peers.any { it.publicKey.equals(peerPublicKey, ignoreCase = true) }) {
                result.add(group.chatId)
            }
        }
        return result
    }

    private suspend fun sendPacket(peerPk: String, jsonText: String) {
        try {
            val packet = SYNC_PACKET_PREFIX + jsonText.toByteArray()
            tox.sendLosslessPacket(PublicKey(peerPk), packet)
        } catch (e: Exception) {
            Log.w(TAG, "Send failed to $peerPk: $e")
        }
    }
}
