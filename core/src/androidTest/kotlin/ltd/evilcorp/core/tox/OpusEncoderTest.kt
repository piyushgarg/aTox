// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class OpusEncoderTest {

    @Test
    fun testNativeCreateAndDestroy() {
        val encoder = OpusEncoder()
        
        // Test standard VoIP sample rates and channels
        val sampleRates = listOf(16000, 48000)
        val channelsList = listOf(1, 2)

        for (sampleRate in sampleRates) {
            for (channels in channelsList) {
                val encoderPtr = encoder.nativeCreate(sampleRate, channels)
                assertTrue(encoderPtr != 0L, "Failed to create OpusEncoder for $sampleRate Hz and $channels channels")
                encoder.nativeDestroy(encoderPtr)
            }
        }
    }

    @Test
    fun testNativeEncode() {
        val encoder = OpusEncoder()
        val sampleRate = 48000
        val channels = 1
        val frameLengthMs = 20
        // frameSize is the number of samples per channel (48000 * 20 / 1000 = 960)
        val frameSize = sampleRate * channels * frameLengthMs / 1000
        
        val encoderPtr = encoder.nativeCreate(sampleRate, channels)
        assertTrue(encoderPtr != 0L, "Failed to create OpusEncoder")

        try {
            // ShortArray filled with zero PCM samples (silence)
            val pcm = ShortArray(frameSize)
            val encodedBytes = encoder.nativeEncode(encoderPtr, pcm, frameSize)

            assertNotNull(encodedBytes, "Encoded bytes should not be null")
            assertTrue(encodedBytes.isNotEmpty(), "Encoded bytes should not be empty")
            // Compressed frame should be significantly smaller than raw PCM (960 shorts * 2 bytes = 1920 bytes)
            assertTrue(encodedBytes.size < frameSize * 2, "Compressed size should be smaller than PCM size")
        } finally {
            encoder.nativeDestroy(encoderPtr)
        }
    }
}
