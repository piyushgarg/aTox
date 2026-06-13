package ltd.evilcorp.atox.infrastructure.tox
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

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
import ltd.evilcorp.domain.features.group.GroupEventBus
import ltd.evilcorp.domain.features.group.GroupDomainEvent
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "GroupSyncManager"
private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}
private val SYNC_PACKET_PREFIX = byteArrayOf(0xA0.toByte())

@Singleton
class GroupSyncManager @Inject constructor(
    private val scope: CoroutineScope,
    private val groupRepository: IGroupRepository,
    private val contactRepository: IContactRepository,
    private val tox: ITox,
    private val groupManager: GroupManager,
    private val groupEventBus: GroupEventBus
) {

    // Store accumulated IDs for multi-page requests (key = chatId|fromPk).
    private val pendingIdPages = ConcurrentHashMap<String, MutableSet<Int>>()

    init {
        groupEventBus.events.onEach { event ->
            if (event is GroupDomainEvent.LocalMessageSent) {
                triggerSyncForGroup(event.chatId)
            }
        }.launchIn(scope)
    }

    // Entry Points

    fun triggerSyncForGroup(chatId: String) {
        scope.launch {
            val contacts = contactRepository.getAll().firstOrNull() ?: emptyList()
            for (contact in contacts) {
                val isBootstrap = groupManager.isBootstrapFriend(contact.publicKey)
                if (!isBootstrap) {
                    sendSummary(chatId, contact.publicKey)
                }
            }
        }
    }

    /**
     * Triggered when a friend comes online. Triggers reconnection or message synchronization.
     */
    fun onGroupPeerOnline(friendPublicKey: String) {
        scope.launch {
            val isBootstrapOnly = groupManager.isBootstrapFriend(friendPublicKey)

            val allGroups = groupRepository.getAll().firstOrNull() ?: emptyList()
            for (group in allGroups) {
                val chatId = group.chatId
                val currentStatus = groupManager.connectionStatus(chatId)
                if (currentStatus == GroupConnectionStatus.Disconnected || currentStatus == GroupConnectionStatus.Connecting) {
                    groupManager.scheduleAutoReconnect(chatId, group.groupNumber)
                }
                
                if (!isBootstrapOnly) {
                    sendSummary(chatId, friendPublicKey)
                }
            }
            if (!isBootstrapOnly) {
                sendGroupRelay(friendPublicKey)
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
                    TYPE_GROUP_RELAY -> onGroupRelayReceived(fromPublicKey, payload)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Parse error: $e")
            }
        }
    }
    // Phase 1 — Summary Exchange (Each side sends: count, lastId)
    private suspend fun sendSummary(chatId: String, peerPk: String) {
        val allIds = groupRepository.getMessageIds(chatId)
        val validIds = allIds.filter { it != -1 } // Exclude unsent (-1)
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
            val myIds = groupRepository.getMessageIds(chatId).filter { it != -1 }
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
    // Phase 2 — Multi-page ID exchange
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
            val myIds = groupRepository.getMessageIds(chatId).filter { it != -1 }
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
    // Phase 3 — Send messages in size-controlled batches
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
        Log.w(TAG, "onMsgPageReceived: payload.messages.size = ${messages.size}")
        var saved = 0
        for (obj in messages) {
            val corrId = obj.correlationId
            val exists = groupRepository.existsByCorrelationId(chatId, corrId)
            Log.w(TAG, "onMsgPageReceived: checking corrId=$corrId, exists=$exists")
            if (exists) continue
            
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
            Log.w(TAG, "onMsgPageReceived: inserting message $corrId '${obj.message}'")
            groupRepository.addMessage(msg)
            saved++
        }
        Log.i(TAG, "Saved $saved synced messages for $chatId from $fromPk (more=$more)")
    }
    // Layer 2+4: Group Relay — invite exchange for stuck groups
    /**
     * Sends our group connection statuses to a friend so they can invite us
     * (if they're Connected) or we can invite them.
     */
    private suspend fun sendGroupRelay(peerPk: String) {
        val allGroups = groupRepository.getAll().firstOrNull() ?: return
        val entries = mutableListOf<GroupRelayEntry>()
        for (group in allGroups) {
            val status = groupManager.connectionStatus(group.chatId)
            val statusStr = when (status) {
                GroupConnectionStatus.Connected -> "connected"
                GroupConnectionStatus.Connecting -> "connecting"
                else -> "disconnected"
            }
            entries.add(GroupRelayEntry(group.chatId, statusStr, group.groupNumber))
        }
        if (entries.isEmpty()) return
        val payload = GroupSyncPayload(
            type = TYPE_GROUP_RELAY,
            groups = entries
        )
        sendPacket(peerPk, json.encodeToString(payload))
        Log.i(TAG, "Sent group relay with ${entries.size} groups to $peerPk")
    }
    /**
     * When we receive relay status from a friend:
     * - If we're Connected and they're connecting → send them a native invite
     * - If they're Connected and we're connecting → do nothing, they should invite us
     */
    private suspend fun onGroupRelayReceived(fromPk: String, payload: GroupSyncPayload) {
        val friendNo = tox.getFriendNumber(PublicKey(fromPk))
        if (friendNo < 0) return
        for (entry in payload.groups) {
            val myStatus = groupManager.connectionStatus(entry.chatId)
            val myGroup = groupRepository.getDirect(entry.chatId) ?: continue
            Log.i(TAG, "Group relay for ${entry.chatId}: friend=$fromPk theirStatus=${entry.status} myStatus=$myStatus")
            // I'm Connected, they're stuck → send them an invite
            if (myStatus == GroupConnectionStatus.Connected && entry.status != "connected") {
                if (myGroup.groupNumber >= 0) {
                    try {
                        val sent = tox.groupInviteSend(myGroup.groupNumber, friendNo)
                        Log.i(TAG, "Layer 2: Sent group invite to $fromPk for ${entry.chatId}: $sent")
                    } catch (e: Exception) {
                        Log.w(TAG, "Layer 2: Failed to send invite to $fromPk for ${entry.chatId}: $e")
                    }
                }
            }
        }
    }
    /**
     * Layer 4: Periodic sweep — if we're Connected to a group, check all online friends
     * who are known peers and send them invites if needed.
     * Call this from an external periodic trigger (e.g., every 60 seconds).
     */
    fun periodicInviteSweep() {
        scope.launch {
            val allGroups = groupRepository.getAll().firstOrNull() ?: return@launch
            for (group in allGroups) {
                val myStatus = groupManager.connectionStatus(group.chatId)
                if (myStatus == GroupConnectionStatus.Connected && group.groupNumber >= 0) {
                    val peers = groupRepository.getPeers(group.chatId).firstOrNull() ?: emptyList()
                    for (peer in peers) {
                        if (!peer.isOurselves && peer.publicKey.isNotEmpty()) {
                            val contact = contactRepository.get(peer.publicKey).firstOrNull()
                            if (contact != null) {
                                val friendNo = tox.getFriendNumber(PublicKey(peer.publicKey))
                                if (friendNo >= 0) {
                                    try {
                                        val sent = tox.groupInviteSend(group.groupNumber, friendNo)
                                        if (sent) {
                                            Log.i(TAG, "Layer 4: Periodic invite sent to ${peer.publicKey} for ${group.chatId}")
                                        }
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
