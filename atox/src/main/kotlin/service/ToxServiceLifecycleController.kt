// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.service

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.tox.ToxBootstrapper
import ltd.evilcorp.atox.util.PermissionManager
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.FriendRequest
import ltd.evilcorp.core.repository.UserRepository
import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.domain.feature.CallState
import ltd.evilcorp.domain.feature.FriendRequestManager
import ltd.evilcorp.domain.tox.Tox
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ToxServiceLC"

@Singleton
class ToxServiceLifecycleController @Inject constructor(
    private val context: Context,
    private val tox: Tox,
    private val userRepository: UserRepository,
    private val friendRequestManager: FriendRequestManager,
    private val callManager: CallManager,
    private val proximityScreenOff: ProximityScreenOff,
    private val permissionManager: PermissionManager,
    private val toxBootstrapper: ToxBootstrapper
) {
    private val notifier = NotificationManagerCompat.from(context)
    private val knownFriendRequests = mutableSetOf<FriendRequest>()

    fun start(lifecycleOwner: LifecycleOwner, onConnectionStatusChanged: (ConnectionStatus) -> Unit) {
        val scope = lifecycleOwner.lifecycleScope
        val lifecycle = lifecycleOwner.lifecycle

        scope.launch(Dispatchers.Default) {
            var lastStatus: ConnectionStatus? = null
            userRepository.get(tox.publicKey.string())
                .filterNotNull()
                .flowWithLifecycle(lifecycle)
                .collect { user ->
                    val connectionStatus = user.connectionStatus
                    if (connectionStatus != lastStatus) {
                        lastStatus = connectionStatus
                        onConnectionStatusChanged(connectionStatus)
                        toxBootstrapper.updateConnectionStatus(connectionStatus)
                    }
                }
        }

        scope.launch(Dispatchers.Default) {
            friendRequestManager.getAll()
                .filterNotNull()
                .flowWithLifecycle(lifecycle)
                .collect { friendRequests ->
                    val finishedFriendRequests = knownFriendRequests.minus(friendRequests.toSet())
                    finishedFriendRequests.forEach {
                        knownFriendRequests.remove(it)
                        notifier.cancel(it.publicKey.hashCode())
                    }
                    knownFriendRequests.addAll(friendRequests)
                }
        }

        scope.launch {
            callManager.inCall.collect {
                if (it is CallState.Active) {
                    if (!callManager.speakerphoneOnState.value) {
                        proximityScreenOff.acquire()
                    }
                } else {
                    proximityScreenOff.release()
                }
            }
        }
    }

    fun stop() {
        toxBootstrapper.cancel()
    }
}
