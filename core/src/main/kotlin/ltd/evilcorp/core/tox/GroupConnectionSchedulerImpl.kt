package ltd.evilcorp.core.tox

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.GroupManager
import ltd.evilcorp.domain.features.group.IGroupConnectionScheduler
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import ltd.evilcorp.domain.core.network.ITox
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val TAG = "GroupConnectionScheduler"

@Singleton
@Suppress("MagicNumber", "MaxLineLength")
class GroupConnectionSchedulerImpl @Inject constructor(
    private val scope: CoroutineScope,
    private val tox: ITox,
    private val groupRepository: IGroupRepository,
    private val groupManagerProvider: Provider<GroupManager>
) : IGroupConnectionScheduler {

    private val reconnectJobs = ConcurrentHashMap<String, Job>()

    override fun cancelReconnect(chatId: String) {
        reconnectJobs.remove(chatId)?.cancel()
    }

    private fun groupManager(): GroupManager = groupManagerProvider.get()

    private suspend fun reconnectWithRetry(chatId: String, groupNumber: Int, maxRetries: Int = 999999) {
        val manager = groupManager()
        for (attempt in 0 until maxRetries) {
            val currentStatus = manager.connectionStatus(chatId)
            // Stop retrying if the group has successfully connected, or reconnect was aborted (e.g. user left the group)
            if (currentStatus == GroupConnectionStatus.Connected || (currentStatus == GroupConnectionStatus.Disconnected && attempt > 0)) {
                Log.d(TAG, "reconnectWithRetry for group $chatId stopped: status is $currentStatus")
                return
            }

            val ok = tox.groupReconnect(groupNumber)
            Log.d(TAG, "Reconnect attempt $attempt for group $chatId returned: $ok")
            if (ok) {
                manager.setConnectionStatus(chatId, GroupConnectionStatus.Connecting)
            }

            // Adaptive delay (Exponential Back-off) to conserve battery and network:
            // - first 10 attempts: every 5 seconds (fast reconnection)
            // - next 20 attempts: every 15 seconds
            // - all subsequent attempts: every 30 seconds
            val delayMs = when {
                attempt < 10 -> 5000L
                attempt < 30 -> 15000L
                else -> 30000L
            }
            delay(delayMs)
        }

        val finalStatus = manager.connectionStatus(chatId)
        if (finalStatus == GroupConnectionStatus.Reconnecting || finalStatus == GroupConnectionStatus.Connecting) {
            Log.e(TAG, "All $maxRetries reconnect attempts failed for group $chatId")
            manager.setConnectionStatus(chatId, GroupConnectionStatus.Disconnected)
        }
    }

    override fun reconnectAll() {
        scope.launch {
            val groups = groupRepository.getAll().firstOrNull() ?: return@launch
            Log.d(TAG, "reconnectAll found ${groups.size} groups in database")
            val manager = groupManager()
            for (group in groups) {
                val currentStatus = manager.connectionStatus(group.chatId)
                Log.d(TAG, "Group ${group.chatId} database status connected: ${group.connected}, current status state: $currentStatus, groupNumber: ${group.groupNumber}")

                if (currentStatus == GroupConnectionStatus.Connected) {
                    continue
                }

                reconnectJobs[group.chatId]?.cancel()

                groupRepository.setConnected(group.chatId, false)
                manager.setConnectionStatus(group.chatId, GroupConnectionStatus.Reconnecting)
                if (group.groupNumber >= 0) {
                    Log.d(TAG, "Launching reconnectWithRetry for group ${group.chatId}")
                    val job = scope.launch {
                        reconnectWithRetry(group.chatId, group.groupNumber)
                    }
                    reconnectJobs[group.chatId] = job
                } else {
                    Log.w(TAG, "Group ${group.chatId} has invalid groupNumber ${group.groupNumber}, setting Disconnected")
                    manager.setConnectionStatus(group.chatId, GroupConnectionStatus.Disconnected)
                }
            }
        }
    }

    override fun scheduleAutoReconnect(chatId: String, groupNumber: Int) {
        reconnectJobs[chatId]?.cancel()
        val manager = groupManager()
        val job = scope.launch {
            delay(3000)
            val g = groupRepository.get(chatId).firstOrNull()
            if (g == null || g.connected) return@launch
            manager.setConnectionStatus(chatId, GroupConnectionStatus.Reconnecting)
            reconnectWithRetry(chatId, groupNumber)
        }
        reconnectJobs[chatId] = job
    }
}
