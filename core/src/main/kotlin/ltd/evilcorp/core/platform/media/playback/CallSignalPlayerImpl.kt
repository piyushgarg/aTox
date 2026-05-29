// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.platform.media.playback

import ltd.evilcorp.domain.features.call.service.ICallSignalPlayer

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CallSignalPlayerImpl"
private const val RINGBACK_TONE_DURATION_MS = 1_500
private const val RINGBACK_TONE_INTERVAL_MS = 2_000L

@Singleton
class CallSignalPlayerImpl @Inject constructor(
    private val context: Context,
    private val userSettingsRepository: IUserSettingsRepository
) : ICallSignalPlayer {
    companion object {
        private const val MAX_VOLUME_PERCENT = 100
        private const val MAX_VOLUME_PERCENT_FLOAT = 100f
    }

    private var ringtone: android.media.Ringtone? = null
    private var toneGenerator: ToneGenerator? = null
    private var ringbackJob: Job? = null

    @Synchronized
    override fun playIncomingRingtone(scope: CoroutineScope) {
        stopSignals()
        scope.launch {
            try {
                val settings = userSettingsRepository.settings.value
                var uri = settings.callRingtoneUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                var rt = RingtoneManager.getRingtone(context, uri)
                if (rt == null) {
                    uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    rt = RingtoneManager.getRingtone(context, uri)
                }
                rt?.let {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        it.isLooping = true
                        val volume = settings.callSoundVolume.coerceIn(0, MAX_VOLUME_PERCENT) / MAX_VOLUME_PERCENT_FLOAT
                        it.volume = volume
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        it.audioAttributes = android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    }
                    it.play()
                    synchronized(this@CallSignalPlayerImpl) {
                        ringtone = it
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing ringtone via RingtoneManager, falling back to default", e)
                try {
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    RingtoneManager.getRingtone(context, defaultUri)?.let { fallbackRt ->
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            fallbackRt.isLooping = true
                        }
                        fallbackRt.play()
                        synchronized(this@CallSignalPlayerImpl) {
                            ringtone = fallbackRt
                        }
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Error playing fallback ringtone", ex)
                }
            }
        }
    }

    @Synchronized
    override fun playRingback(scope: CoroutineScope, isCallActive: () -> Boolean) {
        stopSignals()
        toneGenerator = ToneGenerator(
            AudioManager.STREAM_VOICE_CALL,
            userSettingsRepository.settings.value.callSoundVolume.coerceIn(0, MAX_VOLUME_PERCENT),
        )
        ringbackJob = scope.launch {
            while (isCallActive()) {
                runCatching {
                    synchronized(this@CallSignalPlayerImpl) {
                        toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, RINGBACK_TONE_DURATION_MS)
                    }
                }
                delay(RINGBACK_TONE_INTERVAL_MS)
            }
        }
    }

    @Synchronized
    override fun stopSignals() {
        ringbackJob?.cancel()
        ringbackJob = null
        runCatching {
            toneGenerator?.stopTone()
        }
        toneGenerator?.release()
        toneGenerator = null
        runCatching {
            ringtone?.stop()
        }
        ringtone = null
    }
}
