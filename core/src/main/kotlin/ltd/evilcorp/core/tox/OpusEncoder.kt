// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox

/**
 * JNI wrapper around the native libopus encoder.
 */
class OpusEncoder {
    init {
        System.loadLibrary("nativetox")
    }

    /**
     * Creates an Opus encoder instance.
     * @param sampleRate e.g. 16000 or 48000
     * @param channels e.g. 1 (mono) or 2 (stereo)
     * @return pointer to the native OpusEncoder struct, or 0 on error
     */
    external fun nativeCreate(sampleRate: Int, channels: Int): Long

    /**
     * Encodes a chunk of PCM audio.
     * @param encoderPtr pointer to the native OpusEncoder
     * @param pcm short array containing PCM samples
     * @param frameSize number of samples per channel (e.g. 960 at 48kHz, 20ms)
     * @return encoded Opus packet as ByteArray, or null on error
     */
    external fun nativeEncode(encoderPtr: Long, pcm: ShortArray, frameSize: Int): ByteArray?

    /**
     * Destroys the native Opus encoder and frees its memory.
     * @param encoderPtr pointer to the native OpusEncoder
     */
    external fun nativeDestroy(encoderPtr: Long)
}
