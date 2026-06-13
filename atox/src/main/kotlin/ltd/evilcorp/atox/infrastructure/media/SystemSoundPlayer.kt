package ltd.evilcorp.atox.infrastructure.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_VOLUME = 100
private const val MAX_VOLUME_FLOAT = 100f

@Suppress("unused")
@Singleton
class SystemSoundPlayer @Inject constructor(
    private val context: Context,
) {
    private var activeRingtone: android.media.Ringtone? = null

    fun playNotificationSound(uriString: String, volume: Int) =
        playOneShot(uriString, RingtoneManager.TYPE_NOTIFICATION, AudioAttributes.USAGE_NOTIFICATION_EVENT, volume)

    fun playSentSound(uriString: String, volume: Int) =
        playOneShot(uriString, RingtoneManager.TYPE_NOTIFICATION, AudioAttributes.USAGE_NOTIFICATION_EVENT, volume)

    fun playRingtonePreview(uriString: String, volume: Int) =
        playOneShot(uriString, RingtoneManager.TYPE_RINGTONE, AudioAttributes.USAGE_NOTIFICATION_RINGTONE, volume)

    fun ringtoneTitle(uriString: String, type: Int): String {
        val uri = uriString.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(type)
        return RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: ""
    }

    fun playRingtoneLoop(uriString: String, volume: Int) {
        stopRingtoneLoop()
        val safeVolume = volume.coerceIn(0, MAX_VOLUME)
        val uri = uriString.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        
        runCatching {
            var ringtone = RingtoneManager.getRingtone(context, uri)
            if (ringtone == null) {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ringtone = RingtoneManager.getRingtone(context, fallbackUri)
            }
            ringtone?.let { rt ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    rt.isLooping = true
                    rt.volume = safeVolume / MAX_VOLUME_FLOAT
                }
                rt.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                rt.play()
                activeRingtone = rt
            }
        }.onFailure {
            // Fallback to default ringtone
            runCatching {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                RingtoneManager.getRingtone(context, fallbackUri)?.let { rt ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        rt.isLooping = true
                    }
                    rt.play()
                    activeRingtone = rt
                }
            }
        }
    }

    fun stopRingtoneLoop() {
        runCatching { activeRingtone?.stop() }
        activeRingtone = null
    }

    private fun playOneShot(uriString: String, type: Int, usage: Int, volume: Int) {
        val safeVolume = volume.coerceIn(0, MAX_VOLUME)
        if (safeVolume <= 0) return
        val uri = uriString.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(type)
        runCatching {
            MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                val normalized = safeVolume / MAX_VOLUME_FLOAT
                setVolume(normalized, normalized)
                setOnPreparedListener { it.start() }
                setOnCompletionListener { it.release() }
                prepareAsync()
            }
        }
    }
}
