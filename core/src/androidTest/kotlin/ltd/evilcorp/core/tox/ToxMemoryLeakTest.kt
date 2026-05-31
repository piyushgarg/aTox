// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox

import android.os.Debug
import androidx.test.ext.junit.runners.AndroidJUnit4
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.runtime.ToxWrapper
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.features.settings.model.ProxyType
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ToxMemoryLeakTest {

    private fun getNativeMemory(): Long {
        Runtime.getRuntime().gc()
        System.runFinalization()
        try {
            Thread.sleep(150)
        } catch (e: InterruptedException) {
            // Ignore
        }
        return Debug.getNativeHeapAllocatedSize()
    }

    @Test
    fun testToxWrapperMemoryStability() {
        val iterations = 25
        
        // Take initial memory reading
        val baselineMemory = getNativeMemory()
        
        for (i in 0 until iterations) {
            val options = SaveOptions(null, false, ProxyType.None, "", 0)
            val toxWrapper = ToxWrapper(
                ToxEventListener(),
                ToxAvEventListener(),
                options
            )
            // Perform basic operation to ensure JNI layer is hit
            toxWrapper.setName("Robot $i")
            toxWrapper.getName()
            toxWrapper.close()
        }
        
        // Take final memory reading
        val finalMemory = getNativeMemory()
        val differenceBytes = finalMemory - baselineMemory
        
        // We allow up to 400KB for runtime/GC variations and JNI initialization allocations
        val allowedVarianceBytes = 400 * 1024
        
        assertTrue(
            differenceBytes <= allowedVarianceBytes,
            "Potential JNI memory leak detected: baseline=$baselineMemory, final=$finalMemory, diff=$differenceBytes (allowed: $allowedVarianceBytes)"
        )
    }

    @Test
    fun testOpusEncoderMemoryStability() {
        val iterations = 50
        val sampleRate = 48000
        val channels = 1
        val frameLengthMs = 20
        val frameSize = sampleRate * channels * frameLengthMs / 1000
        val pcm = ShortArray(frameSize)
        
        val baselineMemory = getNativeMemory()
        
        val encoder = OpusEncoder()
        
        for (i in 0 until iterations) {
            val encoderPtr = encoder.nativeCreate(sampleRate, channels)
            assertTrue(encoderPtr != 0L)
            
            // Encode silence
            val encodedBytes = encoder.nativeEncode(encoderPtr, pcm, frameSize)
            assertTrue(encodedBytes != null && encodedBytes.isNotEmpty())
            
            encoder.nativeDestroy(encoderPtr)
        }
        
        val finalMemory = getNativeMemory()
        val differenceBytes = finalMemory - baselineMemory
        
        // We allow up to 50KB for raw OpusEncoder cycles
        val allowedVarianceBytes = 50 * 1024
        
        assertTrue(
            differenceBytes <= allowedVarianceBytes,
            "Potential OpusEncoder native memory leak: baseline=$baselineMemory, final=$finalMemory, diff=$differenceBytes (allowed: $allowedVarianceBytes)"
        )
    }
}
