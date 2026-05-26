package ltd.evilcorp.atox.tox

import android.util.Log
import java.util.EnumSet
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.PublicKey
import ltd.evilcorp.core.model.UserStatus
import ltd.evilcorp.core.model.FINGERPRINT_LEN
import ltd.evilcorp.core.tox.enums.ToxavFriendCallState
import ltd.evilcorp.domain.av.AudioPlayer
import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.domain.tox.ITox

private const val TAG = "CallEventHandler"

private fun String.fingerprint() = this.take(FINGERPRINT_LEN)

class CallEventHandler @Inject constructor(
    private val scope: CoroutineScope,
    private val contactRepository: ContactRepository,
    private val callManager: CallManager,
    private val notificationHelper: NotificationHelper,
    private val tox: ITox,
) {
    private var audioPlayer: AudioPlayer? = null

    private suspend fun tryGetContact(pk: String) = contactRepository.get(pk).firstOrNull().let {
        if (it == null) {
            Log.e(TAG, "Call -> unable to get contact for ${pk.fingerprint()}")
        }
        it
    }

    fun onCall(pk: String, audioEnabled: Boolean, videoEnabled: Boolean) {
        Log.e(TAG, "call ${pk.fingerprint()} $audioEnabled $videoEnabled")
        scope.launch {
            val contact = tryGetContact(pk) ?: Contact(pk)
            notificationHelper.showPendingCallNotification(tox.getStatus(), contact)
            callManager.onIncomingCall(contact)
        }
    }

    fun onCallState(pk: String, callState: EnumSet<ToxavFriendCallState>) {
        Log.e(TAG, "callState ${pk.fingerprint()} $callState")
        if (callState.contains(ToxavFriendCallState.Finished) || callState.contains(ToxavFriendCallState.Error)) {
            audioPlayer?.stop()
            audioPlayer?.release()
            audioPlayer = null
            notificationHelper.dismissCallNotification(PublicKey(pk))
            callManager.terminate(PublicKey(pk))
        } else if (callState.contains(ToxavFriendCallState.SendingAudio) || callState.contains(ToxavFriendCallState.ReceivingAudio)) {
            callManager.onRemoteAnswered(PublicKey(pk))
            callManager.onCallConnected(PublicKey(pk))
        }
    }

    fun onVideoBitRate(pk: String, bitRate: Int) {
        Log.e(TAG, "videoBitRate ${pk.fingerprint()} $bitRate")
    }

    fun onVideoReceiveFrame(
        pk: String,
        width: Int,
        height: Int,
        y: ByteArray,
        u: ByteArray,
        v: ByteArray,
        yStride: Int,
        uStride: Int,
        vStride: Int,
    ) {
        Log.v(
            TAG,
            "videoReceiveFrame ${pk.fingerprint()}$width $height${y.size} ${u.size} ${v.size}$yStride $uStride $vStride",
        )
    }

    fun onAudioBitRate(pk: String, bitRate: Int) {
        Log.e(TAG, "audioBitRate ${pk.fingerprint()} $bitRate")
    }

    fun onAudioReceiveFrame(pcm: ShortArray, channels: Int, samplingRate: Int) {
        if (audioPlayer == null) {
            audioPlayer = AudioPlayer(samplingRate, channels)
            audioPlayer?.start()
        }
        audioPlayer?.buffer(pcm)
    }
}
