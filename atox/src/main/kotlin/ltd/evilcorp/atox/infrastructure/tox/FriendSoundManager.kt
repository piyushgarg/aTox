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
import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.atox.infrastructure.media.SystemSoundPlayer
import ltd.evilcorp.atox.infrastructure.settings.Settings

private const val TAG = "FriendSoundManager"

@Singleton
class FriendSoundManager @Inject constructor(
    private val scope: CoroutineScope,
    private val eventBus: IToxFriendEventBus,
    private val systemSoundPlayer: SystemSoundPlayer,
    private val chatManager: ChatManager,
    private val settings: Settings,
) {
    init {
        scope.launch {
            eventBus.events.collect { event ->
                try {
                    launch(Dispatchers.IO) {
                        processEvent(event)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing event in SoundManager: $event", e)
                }
            }
        }
    }

    private fun processEvent(event: ToxFriendEvent) {
        when (event) {
            is ToxFriendEvent.FriendMessage -> {
                if (chatManager.activeChat != event.publicKey) {
                    systemSoundPlayer.playNotificationSound(
                        settings.notificationSoundUri,
                        settings.notificationSoundVolume
                    )
                } else {
                    systemSoundPlayer.playNotificationSound(
                        settings.activeChatSoundUri,
                        settings.activeChatSoundVolume
                    )
                }
            }
            else -> {}
        }
    }
}
