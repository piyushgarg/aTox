// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.infrastructure.service

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.infrastructure.tox.ToxBootstrapper
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.auth.repository.IUserRepository
import ltd.evilcorp.domain.features.call.CallManager
import ltd.evilcorp.domain.features.call.CallState
import ltd.evilcorp.domain.features.contacts.FriendRequestManager
import ltd.evilcorp.domain.core.network.ITox
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToxServiceLifecycleController @Inject constructor(
    private val context: Context,
    private val tox: ITox,
    private val userRepository: IUserRepository,
    private val friendRequestManager: FriendRequestManager,
    private val callManager: CallManager,
    private val proximityScreenOff: ProximityScreenOff,
    private val toxBootstrapper: ToxBootstrapper
) {
    private val notifier = NotificationManagerCompat.from(context)
    private val knownFriendRequests = mutableSetOf<FriendRequest>()

    fun start(
        lifecycleOwner: LifecycleOwner,
        onConnectionStatusChanged: (ConnectionStatus) -> Unit,
        onCallStateChanged: (Boolean) -> Unit
    ) {
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
                onCallStateChanged(it !is CallState.Idle)
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
