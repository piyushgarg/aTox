package ltd.evilcorp.core.tox

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.bytesToHex
import ltd.evilcorp.domain.core.network.hexToBytes
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.GroupManager
import ltd.evilcorp.domain.features.group.IGroupConnectionScheduler
import ltd.evilcorp.domain.features.group.model.GroupPeer
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val TAG = "GroupConnectionScheduler"
private const val RECONNECT_THROTTLE_ATTEMPTS = 36 // ~3 minutes (36 * 5 seconds)

@Singleton
class GroupConnectionSchedulerImpl @Inject constructor(
    private val scope: CoroutineScope,
    private val tox: ITox,
    private val groupRepository: IGroupRepository,
    private val contactRepository: IContactRepository,
    private val groupManagerProvider: Provider<GroupManager>
) : IGroupConnectionScheduler {

    companion object {
        private const val FAST_RECONNECT_ATTEMPTS = 10
        private const val ADAPTIVE_RECONNECT_ATTEMPTS = 30
        private const val FAST_RECONNECT_DELAY_MS = 5000L
        private const val ADAPTIVE_RECONNECT_DELAY_MS = 15000L
        private const val SLOW_RECONNECT_DELAY_MS = 30000L
        private const val ROOM_TRANSACTION_DELAY_MS = 200L
    }

    private val reconnectJobs = ConcurrentHashMap<String, Job>()
    private val bootstrapFriends = ConcurrentHashMap<String, MutableSet<String>>()

    override fun cancelReconnect(chatId: String) {
        reconnectJobs.remove(chatId)?.cancel()
    }

    override fun stopReconnect(chatId: String) {
        reconnectJobs.remove(chatId)?.cancel()
        scope.launch {
            cleanupBootstrapFriends(chatId)
        }
    }

    override fun isBootstrapFriend(pk: String): Boolean =
        bootstrapFriends.values.any { it.contains(pk.uppercase()) }

    private fun groupManager(): GroupManager = groupManagerProvider.get()

    private suspend fun bootstrapFromKnownPeers(chatId: String, peers: List<GroupPeer>) {
        for (peer in peers) {
            bootstrapPeerIfNecessary(chatId, peer)
        }
    }

    private suspend fun bootstrapPeerIfNecessary(chatId: String, peer: GroupPeer) {
        try {
            val pk = peer.publicKey
            if (pk.isEmpty() || peer.isOurselves) return

            // If already in contacts, do not touch
            val friendNo = tox.getFriendNumber(PublicKey(pk))
            if (friendNo >= 0) {
                Log.d(TAG, "bootstrapPeerIfNecessary: peer $pk is already in contacts (friendNo: $friendNo), skipping bootstrap add")
                return
            }

            val added = tox.addFriendNoRequest(PublicKey(pk))
            if (added >= 0) {
                bootstrapFriends.getOrPut(chatId) { java.util.Collections.newSetFromMap(ConcurrentHashMap()) }.add(pk)
                Log.i(TAG, "Added temporary bootstrap friend $pk for group $chatId (friendNo assigned: $added)")
            } else {
                Log.w(TAG, "Failed to add temporary bootstrap friend $pk: addFriendNoRequest returned $added")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to bootstrap peer $peer: $e")
        }
    }

    private suspend fun cleanupBootstrapFriends(chatId: String) {
        val keys = bootstrapFriends.remove(chatId) ?: return
        for (pk in keys) {
            try {
                // Do not delete if they are a real contact in the repository
                val contact = contactRepository.get(pk).firstOrNull()
                if (contact != null) continue

                val friendNo = tox.getFriendNumber(PublicKey(pk))
                if (friendNo >= 0) {
                    tox.deleteContact(PublicKey(pk))
                    Log.i(TAG, "Removed temporary bootstrap friend $pk for $chatId")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cleanup bootstrap friend $pk: $e")
            }
        }
    }

    private suspend fun executeReconnectAttempt(
        chatId: String,
        currentGroupNumber: Int,
        attempt: Int,
        manager: GroupManager,
        skipReconnectCall: Boolean
    ): Int {
        val currentStatus = manager.connectionStatus(chatId)
        if (currentStatus == GroupConnectionStatus.Connected) {
            Log.d(TAG, "Group $chatId is already Connected, skipping reconnect")
            return currentGroupNumber
        }
        
        // If Connecting, only retry groupReconnect very rarely (every ~3 min)
        // because tox_group_reconnect WIPES the DHT state and restarts discovery from scratch.
        // Calling it too often prevents toxcore from completing peer discovery.
        if (currentStatus == GroupConnectionStatus.Connecting && attempt > 0 && attempt % RECONNECT_THROTTLE_ATTEMPTS != 0) {
            Log.d(TAG, "Group $chatId is in status Connecting, skipping redundant reconnect call for attempt $attempt")
            return currentGroupNumber
        }

        var ok = skipReconnectCall
        var targetGn = currentGroupNumber
        if (targetGn >= 0 && !skipReconnectCall) {
            ok = tox.groupReconnect(targetGn)
        }

        if (!ok) {
            val selfName = manager.getDefaultSelfName()
            val chatIdBytes = chatId.hexToBytes()
            val newGn = tox.groupJoinDirect(chatIdBytes, selfName.toByteArray(), null)
            if (newGn >= 0) {
                Log.i(
                    TAG,
                    "startPersistentReconnect: groupJoinDirect succeeded for $chatId, " +
                        "assigned new groupNumber: $newGn"
                )
                targetGn = newGn
                groupRepository.setGroupNumber(chatId, newGn)
                ok = true
            }
        }

        Log.d(TAG, "Reconnect attempt $attempt for group $chatId (gn: $targetGn) returned: $ok")
        if (ok) {
            manager.setConnectionStatus(chatId, GroupConnectionStatus.Connecting)
        }
        return targetGn
    }

    private suspend fun startPersistentReconnect(chatId: String, groupNumber: Int, justJoined: Boolean = false) {
        val manager = groupManager()
        
        // Bootstrap once from known peers before launching the loop
        val peers = groupRepository.getPeers(chatId).firstOrNull() ?: emptyList()
        bootstrapFromKnownPeers(chatId, peers)

        var currentGroupNumber = groupNumber
        var attempt = 0
        while (true) {
            try {
                val g = groupRepository.get(chatId).firstOrNull()
                if (g == null || g.connected) {
                    Log.d(TAG, "startPersistentReconnect for group $chatId stopped: connected or deleted")
                    return
                }

                currentGroupNumber = executeReconnectAttempt(chatId, currentGroupNumber, attempt, manager, justJoined && attempt == 0)
            } catch (e: Exception) {
                Log.w(TAG, "Reconnect attempt $attempt failed for $chatId: $e")
            }

            // Adaptive back-off delay:
            // - first 10 attempts: every 5 seconds (fast reconnection)
            // - next 20 attempts: every 15 seconds
            // - all subsequent attempts: every 30 seconds
            val delayMs = when {
                attempt < FAST_RECONNECT_ATTEMPTS -> FAST_RECONNECT_DELAY_MS
                attempt < ADAPTIVE_RECONNECT_ATTEMPTS -> ADAPTIVE_RECONNECT_DELAY_MS
                else -> SLOW_RECONNECT_DELAY_MS
            }
            delay(delayMs)
            attempt++
        }
    }

    private suspend fun syncGroupNumbers(): Map<String, Int> {
        val syncedNumbers = mutableMapOf<String, Int>()
        try {
            val toxGroupNumbers = tox.groupGetChatlist()
            Log.d(TAG, "syncGroupNumbers: toxcore has ${toxGroupNumbers.size} active groups")
            for (gn in toxGroupNumbers) {
                val chatIdBytes = tox.groupGetChatId(gn) ?: continue
                val groupChatId = chatIdBytes.bytesToHex().lowercase()
                syncedNumbers[groupChatId] = gn
                val dbGroup = groupRepository.getDirect(groupChatId)
                if (dbGroup != null && dbGroup.groupNumber != gn) {
                    Log.i(
                        TAG,
                        "syncGroupNumbers: updating groupNumber for $groupChatId: " +
                            "${dbGroup.groupNumber} -> $gn"
                    )
                    groupRepository.setGroupNumber(groupChatId, gn)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncGroupNumbers failed: $e")
        }
        return syncedNumbers
    }

    override fun reconnectAll() {
        scope.launch {
            val syncedNumbers = syncGroupNumbers()
            delay(ROOM_TRANSACTION_DELAY_MS) // Wait for Room to apply groupNumber transactions and update Flow
            
            // Get active group list from C core to see what needs registration
            val activeGroupNumbers = try {
                tox.groupGetChatlist().toList()
            } catch (e: Exception) {
                emptyList<Int>()
            }
            val activeGroupIds = activeGroupNumbers.mapNotNull { gn ->
                try {
                    tox.groupGetChatId(gn)?.bytesToHex()?.lowercase()
                } catch (e: Exception) {
                    null
                }
            }.toSet()
            
            val groups = groupRepository.getAll().firstOrNull() ?: return@launch
            Log.d(TAG, "reconnectAll found ${groups.size} groups in database")
            val manager = groupManager()
            for (group in groups) {
                val actualGroupNumber = syncedNumbers[group.chatId] ?: group.groupNumber
                group.groupNumber = actualGroupNumber
                reconnectSingleGroup(group, activeGroupNumbers, activeGroupIds, manager)
            }
        }
    }

    private suspend fun reconnectSingleGroup(
        group: ltd.evilcorp.domain.features.group.model.Group,
        activeGroupNumbers: List<Int>,
        activeGroupIds: Set<String>,
        manager: GroupManager
    ) {
        val currentStatus = manager.connectionStatus(group.chatId)
        Log.d(
            TAG,
            "Group ${group.chatId} database status connected: ${group.connected}, " +
                "current status state: $currentStatus, groupNumber: ${group.groupNumber}"
        )

        if (currentStatus == GroupConnectionStatus.Connected ||
            currentStatus == GroupConnectionStatus.Connecting) {
            return
        }

        reconnectJobs[group.chatId]?.cancel()

        groupRepository.setConnected(group.chatId, false)
        manager.setConnectionStatus(group.chatId, GroupConnectionStatus.Reconnecting)
        
        var targetGn = group.groupNumber
        val isActiveInCore = targetGn >= 0 &&
            activeGroupNumbers.contains(targetGn) &&
            activeGroupIds.contains(group.chatId)
        
        var justJoined = false
        if (!isActiveInCore) {
            try {
                val selfName = manager.getDefaultSelfName()
                val chatIdBytes = group.chatId.hexToBytes()
                val newGn = tox.groupJoinDirect(chatIdBytes, selfName.toByteArray(), null)
                if (newGn >= 0) {
                    targetGn = newGn
                    groupRepository.setGroupNumber(group.chatId, newGn)
                    justJoined = true
                    Log.i(
                        TAG,
                        "Group ${group.chatId} re-registered natively, " +
                            "assigned new groupNumber: $newGn"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Re-registration failed for group ${group.chatId}: $e")
            }
        }
        
        if (targetGn >= 0) {
            Log.d(
                TAG,
                "Launching reconnect for group ${group.chatId} with groupNumber: $targetGn"
            )
            val job = scope.launch {
                startPersistentReconnect(group.chatId, targetGn, justJoined)
            }
            reconnectJobs[group.chatId] = job
        } else {
            Log.w(TAG, "Group ${group.chatId} has invalid groupNumber, setting Disconnected")
            manager.setConnectionStatus(group.chatId, GroupConnectionStatus.Disconnected)
        }
    }

    override fun scheduleAutoReconnect(chatId: String, groupNumber: Int) {
        val manager = groupManager()
        val currentStatus = manager.connectionStatus(chatId)
        if (currentStatus == GroupConnectionStatus.Connecting || currentStatus == GroupConnectionStatus.Reconnecting) {
            Log.d(TAG, "scheduleAutoReconnect: Group $chatId is already $currentStatus, skipping to avoid interruption")
            return
        }

        reconnectJobs[chatId]?.cancel()
        manager.setConnectionStatus(chatId, GroupConnectionStatus.Reconnecting)
        val job = scope.launch {
            startPersistentReconnect(chatId, groupNumber)
        }
        reconnectJobs[chatId] = job
    }
}
