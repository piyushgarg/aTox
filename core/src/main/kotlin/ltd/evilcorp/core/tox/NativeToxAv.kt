package ltd.evilcorp.core.tox

import ltd.evilcorp.core.tox.listener.ToxAvEventListener

class NativeToxAv {
    init {
        System.loadLibrary("nativetox")
    }

    external fun toxavNew(tox: Long): Long
    external fun toxavKill(toxav: Long)

    external fun toxavIterate(toxav: Long, listener: ToxAvEventListener)
    external fun toxavIterationInterval(toxav: Long): Int

    external fun toxavCall(toxav: Long, friendNumber: Int, audioBitRate: Int, videoBitRate: Int): Boolean
    external fun toxavAnswer(toxav: Long, friendNumber: Int, audioBitRate: Int, videoBitRate: Int): Boolean
    external fun toxavCallControl(toxav: Long, friendNumber: Int, control: Int): Boolean
    
    external fun toxavAudioSendFrame(toxav: Long, friendNumber: Int, pcm: ShortArray, sampleCount: Int, channels: Int, samplingRate: Int): Boolean
}
