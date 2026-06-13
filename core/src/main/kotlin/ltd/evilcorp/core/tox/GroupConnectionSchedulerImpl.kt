// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.core.network.hexToBytes
import ltd.evilcorp.domain.features.group.GroupConnectionStatus
import ltd.evilcorp.domain.features.group.IGroupConnectionScheduler
import ltd.evilcorp.domain.features.group.GroupManager
import ltd.evilcorp.domain.features.group.repository.IGroupRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider

private const val TAG = "GroupConnectionScheduler"

class GroupConnectionSchedulerImpl @Inject constructor(
    private val scope: CoroutineScope,
    private val tox: ITox,
    private val groupRepository: IGroupRepository,
    private val groupManagerProvider: Provider<GroupManager>
) : IGroupConnectionScheduler {

    private val manager: GroupManager get() = groupManagerProvider.get()
    internal val reconnectJobs = ConcurrentHashMap<String, Job>()

    override fun cancelReconnect(chatId: String) {
        val job = reconnectJobs[chatId]
        if (job != null) {
            job.cancel()
            reconnectJobs.remove(chatId)
            Log.d(TAG, "Canceled auto-reconnect loop for group $chatId")
        }
        cleanupBootstrapFriends()
    }

    override fun stopReconnect(chatId: String) {
        cancelReconnect(chatId)
        manager.removeConnectionStatus(chatId)
        scope.launch {
            groupRepository.setConnected(chatId, false)
        }
    }

    override fun scheduleAutoReconnect(chatId: String, groupNumber: Int) {
        val existingJob = reconnectJobs[chatId]
        if (existingJob != null) {
            existingJob.cancel()
            Log.i(TAG, "scheduleAutoReconnect: canceling existing loop and restarting for group $chatId")
        } else {
            Log.i(TAG, "scheduleAutoReconnect: starting loop for group $chatId")
        }

        val job = scope.launch {
            startPersistentReconnect(chatId, groupNumber)
        }
        reconnectJobs[chatId] = job
    }

    override fun reconnectAll() {
        Log.i(TAG, "reconnectAll triggered")
        scope.launch {
            val groups = groupRepository.getAll().firstOrNull() ?: return@launch
            for (group in groups) {
                if (!group.connected) {
                    reconnectSingleGroup(group.chatId, group.groupNumber)
                }
            }
        }
    }

    override fun isBootstrapFriend(pk: String): Boolean {
        // Find if this public key is temporarily added to any group as a bootstrap friend
        // In the future we might want to track this more explicitly
        return false // We rely on contact repository to filter them out in the UI
    }

    private fun reconnectSingleGroup(chatId: String, groupNumber: Int) {
        if (reconnectJobs.containsKey(chatId)) {
            Log.w(TAG, "reconnectSingleGroup: loop already exists for group $chatId")
            return
        }

        val job = scope.launch {
            startPersistentReconnect(chatId, groupNumber)
        }
        reconnectJobs[chatId] = job
    }

    private suspend fun startPersistentReconnect(chatId: String, groupNumber: Int) {
        Log.d(TAG, "startPersistentReconnect: single attempt for $chatId")

        try {
            executeReconnectAttempt(chatId, groupNumber, 0, manager, skipReconnectCall = false)
        } catch (e: Exception) {
            Log.w(TAG, "Reconnect attempt failed for $chatId: $e")
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

    private fun cleanupBootstrapFriends() {
        // No-op now as we removed bootstrap friends logic
    }
}
