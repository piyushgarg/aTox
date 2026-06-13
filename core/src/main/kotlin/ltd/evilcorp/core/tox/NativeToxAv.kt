package ltd.evilcorp.core.tox

import ltd.evilcorp.core.tox.listener.ToxAvEventListener

class NativeToxAv {
    companion object {
        init {
            System.loadLibrary("nativetox")
        }
    }

    external fun toxavNew(tox: Long): Long
    external fun toxavKill(toxav: Long)

    external fun toxavIterate(toxav: Long, listener: ToxAvEventListener)
    external fun toxavIterationInterval(toxav: Long): Int

    external fun toxavCall(toxav: Long, friendNumber: Int, audioBitRate: Int, videoBitRate: Int): Boolean
    external fun toxavAnswer(toxav: Long, friendNumber: Int, audioBitRate: Int, videoBitRate: Int): Boolean
    external fun toxavCallControl(toxav: Long, friendNumber: Int, control: Int): Boolean
    
    external fun toxavAudioSendFrame(toxav: Long, friendNumber: Int, pcm: ShortArray, sampleCount: Int, channels: Int, samplingRate: Int): Boolean

    // ===================================================================================
    // Video frames transmission and calls bitrate regulation
    // ===================================================================================

    /**
     * Sends a video frame (YUV420P) to your peer in an active video call.
     * @param toxav Pointer to the native ToxAV instance.
     * @param friendNumber Friend number.
     * @param width Width of the video frame.
     * @param height Height of the video frame.
     * @param y Y plane array (luminance).
     * @param u U plane array (chrominance blue difference).
     * @param v V plane array (chrominance red difference).
     * @return true on success, false on error.
     */
    external fun toxavVideoSendFrame(toxav: Long, friendNumber: Int, width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray): Boolean

    /**
     * Changes the audio bitrate on the fly for the current call connection.
     * @param toxav Pointer to the native ToxAV instance.
     * @param friendNumber Friend number.
     * @param bitrate New audio bitrate in bps.
     * @return true on success, false on error.
     */
    external fun toxavAudioSetBitRate(toxav: Long, friendNumber: Int, bitrate: Int): Boolean

    /**
     * Changes the video bitrate on the fly for the current call connection.
     * @param toxav Pointer to the native ToxAV instance.
     * @param friendNumber Friend number.
     * @param bitrate New video bitrate in bps.
     * @return true on success, false on error.
     */
    external fun toxavVideoSetBitRate(toxav: Long, friendNumber: Int, bitrate: Int): Boolean

    // ===================================================================================
    // Audio/Video group chats (ToxAV Group API)
    // ===================================================================================

    /**
     * Creates a group audio conference based on an existing Tox instance.
     * @param tox Pointer to the native Tox instance.
     * @return The number of the created voice chat, or -1 on error.
     */
    external fun toxavAddAvGroupchat(tox: Long): Int

    /**
     * Joins an existing group audio conference.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Tox group number.
     * @return The number of the joined audio group, or -1 on error.
     */
    external fun toxavJoinAvGroupchat(tox: Long, groupNumber: Int): Int

    /**
     * Sends an audio frame of your voice to the group chat.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Tox group number.
     * @param pcm Audio data array (PCM 16-bit).
     * @param sampleCount Count of audio samples.
     * @param channels Count of channels (usually 1 - mono, or 2 - stereo).
     * @param samplingRate Audio sampling rate (e.g., 48000).
     * @return 0 on success, or an error code.
     */
    external fun toxavGroupSendAudio(tox: Long, groupNumber: Int, pcm: ShortArray, sampleCount: Int, channels: Int, samplingRate: Int): Int

    /**
     * Enables audio/video functions for the specified group chat.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @return 0 on success, or an error code.
     */
    external fun toxavGroupchatEnableAv(tox: Long, groupNumber: Int): Int

    /**
     * Disables audio/video functions for the specified group chat.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @return 0 on success, or an error code.
     */
    external fun toxavGroupchatDisableAv(tox: Long, groupNumber: Int): Int

    /**
     * Checks if audio/video functions are active in the specified group chat.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @return true if active, false if disabled or an error occurred.
     */
    external fun toxavGroupchatAvEnabled(tox: Long, groupNumber: Int): Boolean
}
