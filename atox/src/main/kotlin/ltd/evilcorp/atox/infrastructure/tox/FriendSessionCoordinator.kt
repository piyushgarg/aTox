// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.infrastructure.tox

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.contacts.IToxFriendEventBus
import ltd.evilcorp.domain.features.contacts.model.ToxFriendEvent
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.transfer.FileTransferManager
import ltd.evilcorp.domain.features.transfer.sendAvatar
import ltd.evilcorp.domain.features.transfer.resetForContact
import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import ltd.evilcorp.domain.features.group.GroupConnectionService

private const val TAG = "FriendSessionCoordinator"

@Singleton
class FriendSessionCoordinator @Inject constructor(
    private val scope: CoroutineScope,
    private val eventBus: IToxFriendEventBus,
    private val fileTransferManager: FileTransferManager,
    private val chatManager: ChatManager,
    private val messageRepository: IMessageRepository,
    private val groupSyncManager: GroupSyncManager,
    private val groupConnectionService: GroupConnectionService,
) {
    init {
        scope.launch {
            eventBus.events.collect { event ->
                try {
                    launch(Dispatchers.IO) {
                        processEvent(event)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing event in SessionCoordinator: $event", e)
                }
            }
        }
    }

    private suspend fun processEvent(event: ToxFriendEvent) {
        when (event) {
            is ToxFriendEvent.FriendConnectionStatus -> {
                if (event.status != ConnectionStatus.None) {
                    groupSyncManager.onGroupPeerOnline(event.publicKey)
                    fileTransferManager.sendAvatar(event.publicKey)
                    val pending = messageRepository.getPending(event.publicKey)
                    if (pending.isNotEmpty()) {
                        chatManager.resend(pending)
                    }
                } else {
                    fileTransferManager.resetForContact(event.publicKey)
                }
            }
            is ToxFriendEvent.SelfConnectionStatus -> {
                if (event.status != ConnectionStatus.None) {
                    groupConnectionService.reconnectAll()
                }
            }
            else -> {}
        }
    }
}
