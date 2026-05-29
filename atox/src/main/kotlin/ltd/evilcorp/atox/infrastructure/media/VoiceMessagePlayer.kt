package ltd.evilcorp.atox.infrastructure.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

private const val TAG = "VoiceMessagePlayer"

object VoiceMessagePlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingUri: String? = null

    fun play(context: Context, uriString: String, onComplete: () -> Unit, onError: () -> Unit) {
        try {
            stop()
            val uri = Uri.parse(uriString)
            mediaPlayer = MediaPlayer().apply {
                if (uri.scheme == "content" || uri.scheme == "file") {
                    setDataSource(context, uri)
                } else {
                    val file = java.io.File(uriString)
                    setDataSource(context, Uri.fromFile(file))
                }
                prepare()
                setOnCompletionListener {
                    onComplete()
                    stop()
                }
                start()
            }
            currentPlayingUri = uriString
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play voice message", e)
            onError()
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            // Ignore
        }
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null
        currentPlayingUri = null
    }

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    fun getDuration(): Int = mediaPlayer?.duration ?: 0
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
    fun isPlayingUri(uriString: String): Boolean = currentPlayingUri == uriString
}
