package ltd.evilcorp.domain.feature

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ltd.evilcorp.core.model.GroupMessage
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.Sender
import ltd.evilcorp.core.repository.GroupRepository
import ltd.evilcorp.domain.tox.Tox
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "GroupSyncManager"

private val SYNC_PACKET_PREFIX = byteArrayOf(0xA0.toByte())

private const val TYPE_SUMMARY = "group_sync_summary"
private const val TYPE_IDS_PAGE = "group_sync_ids_page"
private const val TYPE_MSG_PAGE = "group_sync_msg_page"

// Лимиты для lossless-пакета Tox (~1373 байт).  IDs укладываем в ~900 байт,
// сообщения — тоже в ~900 с поштучной проверкой.
private const val MAX_IDS_PER_PAGE = 80
private const val MSG_BYTE_BUDGET = 900

// Задержка между страницами ID-обмена (ms), чтобы не флудить lossless-канал
private const val PAGE_DELAY_MS = 200L
private const val RE_SYNC_DELAY_MS = 800L

/** Хранилище накопленных ID для многостраничных запросов (ключ = chatId|fromPk). */
private val pendingIdPages = java.util.concurrent.ConcurrentHashMap<String, MutableSet<Int>>()

@Singleton
class GroupSyncManager @Inject constructor(
    private val scope: CoroutineScope,
    private val groupRepository: GroupRepository,
    private val tox: Tox,
    private val groupManager: GroupManager
) {
    // ========================================================================
    // Входные точки
    // ========================================================================

    /** Друг появился онлайн → запускаем sync для каждой общей группы. */

    fun onGroupPeerOnline(friendPublicKey: String) {
        scope.launch {
            val isBootstrapOnly = groupManager.isBootstrapFriend(friendPublicKey)
            val groups = findGroupsWithPeer(friendPublicKey)
            for (chatId in groups) {
                val group = groupRepository.get(chatId).firstOrNull() ?: continue
                if (!group.connected && group.groupNumber >= 0) {
                    tox.groupReconnect(group.groupNumber)
                }
                if (!isBootstrapOnly) {
                    sendSummary(chatId, friendPublicKey) // sync только с реальными участниками
                }
            }
        }
    }

    /** Входящий lossless-пакет с sync-протоколом. */
    fun handleLosslessPacket(fromPublicKey: String, data: ByteArray) {
        if (data.size < 2 || data[0] != SYNC_PACKET_PREFIX[0]) return
        val text = try {
            data.copyOfRange(1, data.size).decodeToString()
        } catch (e: Exception) {
            return
        }

        scope.launch {
            try {
                val json = JSONObject(text)
                when (json.optString("type", "")) {
                    TYPE_SUMMARY -> onSummaryReceived(fromPublicKey, json)
                    TYPE_IDS_PAGE -> onIdsPageReceived(fromPublicKey, json)
                    TYPE_MSG_PAGE -> onMsgPageReceived(fromPublicKey, json)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Parse error: $e")
            }
        }
    }

    // ========================================================================
    // Фаза 1 — обмен сводками: каждая сторона посылает { count, lastId }
    // ========================================================================

    private suspend fun sendSummary(chatId: String, peerPk: String) {
        val allIds = groupRepository.getMessageIds(chatId)
        val validIds = allIds.filter { it >= 0 } // исключаем unsent (-1)
        val lastId = validIds.maxOrNull() ?: return // пустая группа — нечего синкать

        val json = JSONObject()
        json.put("type", TYPE_SUMMARY)
        json.put("chatId", chatId)
        json.put("count", validIds.size)
        json.put("lastId", lastId)
        sendPacket(peerPk, json.toString())

        Log.i(TAG, "Summary sent for $chatId: count=${validIds.size}, lastId=$lastId")
    }

    private fun onSummaryReceived(fromPk: String, json: JSONObject) {
        val chatId = json.optString("chatId", "") ?: return
        val theirCount = json.optInt("count", -1)
        val theirLastId = json.optInt("lastId", -1)
        if (chatId.isEmpty() || theirCount < 0 || theirLastId < 0) return

        scope.launch {
            val myIds = groupRepository.getMessageIds(chatId).filter { it >= 0 }
            val myCount = myIds.size
            val myLastId = myIds.maxOrNull() ?: -1

            // Если сводки совпадают — считаем себя синхронизированными
            if (myCount == theirCount && myLastId == theirLastId) {
                Log.i(TAG, "In-sync for $chatId (count=$myCount, lastId=$myLastId)")
                return@launch
            }

            Log.i(TAG, "Mismatch for $chatId: local(count=$myCount, lastId=$myLastId) vs remote(count=$theirCount, lastId=$theirLastId)")

            // Отвечаем первой страницей наших ID
            val sorted = myIds.sorted()
            val page = sorted.take(MAX_IDS_PER_PAGE)
            val hasMore = sorted.size > MAX_IDS_PER_PAGE
            pendingIdPages["$chatId|$fromPk"] = mutableSetOf<Int>() // сброс накопленного

            sendIdsPage(fromPk, chatId, page, 0, hasMore)
        }
    }

    // ========================================================================
    // Фаза 2 — постраничный обмен ID. Получатель накапливает ID, и когда
    // приходит последняя страница — отвечает сообщениями.
    // ========================================================================

    private suspend fun sendIdsPage(toPk: String, chatId: String, ids: List<Int>, pageNum: Int, more: Boolean) {
        val json = JSONObject()
        json.put("type", TYPE_IDS_PAGE)
        json.put("chatId", chatId)
        json.put("page", pageNum)
        json.put("more", more)
        val arr = JSONArray()
        ids.forEach { arr.put(it) }
        json.put("ids", arr)
        sendPacket(toPk, json.toString())
    }

    private fun onIdsPageReceived(fromPk: String, json: JSONObject) {
        val chatId = json.optString("chatId", "") ?: return
        val page = json.optInt("page", 0)
        val more = json.optBoolean("more", false)
        val idsArr = json.optJSONArray("ids") ?: return

        // Запоминаем полученные ID
        val accumulator = pendingIdPages.getOrPut("$chatId|$fromPk") { mutableSetOf() }
        for (i in 0 until idsArr.length()) {
            accumulator.add(idsArr.getInt(i))
        }

        Log.i(TAG, "Received IDs page $page for $chatId (accumulated=${accumulator.size}, more=$more)")

        if (more) {
            // Отправитель пришлёт ещё страницы — ждём
            return
        }

        // ====== Все страницы получены ======
        scope.launch {
            val remoteIds = accumulator.toList()
            pendingIdPages.remove("$chatId|$fromPk")

            val myIds = groupRepository.getMessageIds(chatId).filter { it >= 0 }
            val mySet = myIds.toSet()

            // Чего нет у меня, но есть у них → запрашивать не будем (пусть они нам пришлют),
            // а вот чего нет у них, но есть у меня → отправляем
            val theyNeed = myIds.filter { it !in remoteIds.toSet() }
            if (theyNeed.isEmpty()) {
                Log.i(TAG, "No messages to send for $chatId")
                return@launch
            }

            Log.i(TAG, "Sending ${theyNeed.size} missing messages for $chatId to $fromPk")
            sendMsgPages(fromPk, chatId, theyNeed.sorted())
        }
    }

    // ========================================================================
    // Фаза 3 — отправка сообщений чанками с контролем размера
    // ========================================================================

    private suspend fun sendMsgPages(toPk: String, chatId: String, msgIds: List<Int>) {
        var index = 0
        val total = msgIds.size
        while (index < total) {
            val batchMsgs = mutableListOf<JSONObject>()
            var byteEstimate = 0

            while (index < total) {
                val id = msgIds[index]
                val msgs = groupRepository.getMessagesByIds(chatId, setOf(id))
                if (msgs.isEmpty()) { index++; continue }

                val msg = msgs.first()
                val obj = JSONObject()
                obj.put("peerId", msg.peerId)
                obj.put("senderName", msg.senderName)
                obj.put("message", msg.message)
                obj.put("timestamp", msg.timestamp)
                obj.put("correlationId", msg.correlationId)
                obj.put("type", msg.type.name)

                val cost = obj.toString().toByteArray().size + 1
                if (batchMsgs.isNotEmpty() && byteEstimate + cost > MSG_BYTE_BUDGET) break

                batchMsgs.add(obj)
                byteEstimate += cost
                index++
            }

            val more = index < total
            val json = JSONObject()
            json.put("type", TYPE_MSG_PAGE)
            json.put("chatId", chatId)
            json.put("more", more)
            val arr = JSONArray()
            batchMsgs.forEach { arr.put(it) }
            json.put("messages", arr)

            sendPacket(toPk, json.toString())
            if (more) delay(PAGE_DELAY_MS) // небольшая пауза между страницами
        }
    }

    private fun onMsgPageReceived(fromPk: String, json: JSONObject) {
        val chatId = json.optString("chatId", "") ?: return
        val more = json.optBoolean("more", false)
        val arr = json.optJSONArray("messages") ?: return

        var saved = 0
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val corrId = obj.getInt("correlationId")
            if (groupRepository.existsByCorrelationId(chatId, corrId)) continue

            val msg = GroupMessage(
                groupChatId = chatId,
                peerId = obj.getInt("peerId"),
                senderName = obj.getString("senderName"),
                message = obj.getString("message"),
                sender = Sender.Received,
                type = try { MessageType.valueOf(obj.getString("type")) }
                        catch (e: Exception) { MessageType.Normal },
                correlationId = corrId,
                timestamp = obj.getLong("timestamp"),
            )
            groupRepository.addMessage(msg)
            saved++
        }
        Log.i(TAG, "Saved $saved synced messages for $chatId (more=$more)")
    }

    // ========================================================================
    // Поиск групп с участником
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

    // ========================================================================
    // Отправка lossless-пакета
    // ========================================================================

    private suspend fun sendPacket(peerPk: String, jsonText: String) {
        try {
            val packet = SYNC_PACKET_PREFIX + jsonText.toByteArray()
            tox.sendLosslessPacket(PublicKey(peerPk), packet)
        } catch (e: Exception) {
            Log.w(TAG, "Send failed to $peerPk: $e")
        }
    }
}
