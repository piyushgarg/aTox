package ltd.evilcorp.atox.infrastructure.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import ltd.evilcorp.domain.feature.IAudioRoutingManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRoutingManager @Inject constructor(
    private val context: Context
) : IAudioRoutingManager {

    private val audioManager = ContextCompat.getSystemService(context, AudioManager::class.java)
    private var focusRequest: AudioFocusRequest? = null
    private var activeFocusChangeListener: AudioManager.OnAudioFocusChangeListener? = null

    override fun requestCallAudioFocus(onFocusLoss: () -> Unit, onFocusGain: () -> Unit): Boolean {
        if (audioManager == null) return false
        
        val listener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    onFocusLoss()
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    onFocusGain()
                }
            }
        }
        activeFocusChangeListener = listener

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(listener)
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                listener,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    override fun abandonCallAudioFocus() {
        val am = audioManager ?: return
        val listener = activeFocusChangeListener ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { am.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(listener)
        }
        activeFocusChangeListener = null
    }

    override fun setSpeakerphoneRoute(on: Boolean) {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (on) {
                val speakerDevice = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    ?.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                if (speakerDevice != null) {
                    am.setCommunicationDevice(speakerDevice)
                }
            } else {
                am.clearCommunicationDevice()
            }
        } else {
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn = on
        }
    }

    override fun setCommunicationMode(active: Boolean) {
        audioManager?.mode = if (active) {
            AudioManager.MODE_IN_COMMUNICATION
        } else {
            AudioManager.MODE_NORMAL
        }
    }
}
