package ltd.evilcorp.atox.infrastructure.media

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.domain.features.settings.model.AppSound

private const val MAX_VOLUME = 100

@Singleton
class SoundEffectPlayer @Inject constructor() {
    fun play(
        sound: AppSound,
        volume: Int,
        stream: Int = AudioManager.STREAM_NOTIFICATION,
        durationMs: Int = 80,
    ) {
        val safeVolume = volume.coerceIn(0, MAX_VOLUME)
        if (safeVolume <= 0) return

        runCatching {
            val generator = ToneGenerator(stream, safeVolume)
            try {
                generator.startTone(sound.toTone(), durationMs)
            } finally {
                generator.release()
            }
        }
    }

    private fun AppSound.toTone(): Int = when (this) {
        AppSound.SoftPop -> ToneGenerator.TONE_PROP_BEEP2
        AppSound.SoftTick -> ToneGenerator.TONE_PROP_ACK
        AppSound.SoftBeep -> ToneGenerator.TONE_PROP_BEEP
        AppSound.Glass -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
        AppSound.Pulse -> ToneGenerator.TONE_CDMA_ONE_MIN_BEEP
        AppSound.Ripple -> ToneGenerator.TONE_CDMA_PIP
    }
}
