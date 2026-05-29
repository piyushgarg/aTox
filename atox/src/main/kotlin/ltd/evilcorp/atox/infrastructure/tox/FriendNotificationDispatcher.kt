// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.infrastructure.tox

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.contacts.IToxFriendEventBus
import ltd.evilcorp.domain.features.contacts.model.ToxFriendEvent
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import ltd.evilcorp.domain.core.network.ITox
import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.atox.ui.NotificationHelper

private const val TAG = "FriendNotificationDispatcher"

@Singleton
class FriendNotificationDispatcher @Inject constructor(
    private val scope: CoroutineScope,
    private val eventBus: IToxFriendEventBus,
    private val notificationHelper: NotificationHelper,
    private val contactRepository: IContactRepository,
    private val chatManager: ChatManager,
    private val tox: ITox,
) {
    init {
        scope.launch {
            eventBus.events.collect { event ->
                try {
                    launch(Dispatchers.IO) {
                        processEvent(event)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing event in NotificationDispatcher: $event", e)
                }
            }
        }
    }

    private suspend fun processEvent(event: ToxFriendEvent) {
        val isBusy = try {
            tox.getStatus() == UserStatus.Busy
        } catch (e: Exception) {
            false
        }

        when (event) {
            is ToxFriendEvent.FriendRequest -> {
                val request = FriendRequest(event.publicKey, event.message)
                notificationHelper.showFriendRequestNotification(request, silent = isBusy)
            }
            is ToxFriendEvent.FriendMessage -> {
                if (chatManager.activeChat != event.publicKey) {
                    val contact = contactRepository.get(event.publicKey).firstOrNull() ?: Contact(event.publicKey)
                    notificationHelper.showMessageNotification(contact, event.message, silent = isBusy)
                }
            }
            else -> {}
        }
    }
}
