// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.platform.media.recording

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class VoiceRecorderImplTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Test
    fun testStartStopRecording() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = VoiceRecorderImpl(context, Dispatchers.Unconfined)

        // Start voice message recording
        val startSuccess = recorder.startRecording()
        assertTrue(startSuccess, "Voice recording should start successfully")

        // Wait a short time to capture audio frames and compress them
        kotlinx.coroutines.delay(1000)

        // Stop recording
        val filePath = recorder.stopRecording()
        assertNotNull(filePath, "Stop recording should return a non-null path to the saved file")

        val file = File(filePath)
        assertTrue(file.exists(), "The voice recording file should exist on disk")
        assertTrue(file.length() > 0, "The voice recording file size should be greater than zero")

        // Verify that the file has a valid Ogg container format (starts with "OggS")
        val headerBytes = ByteArray(4)
        FileInputStream(file).use { it.read(headerBytes) }
        assertEquals("OggS", String(headerBytes, Charsets.US_ASCII), "File should begin with OggS capture pattern")
        
        // Cleanup file
        file.delete()
    }

    @Test
    fun testCancelRecording() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = VoiceRecorderImpl(context, Dispatchers.Unconfined)

        // Start recording
        val startSuccess = recorder.startRecording()
        assertTrue(startSuccess, "Voice recording should start successfully")

        kotlinx.coroutines.delay(500)

        // Cancel recording
        recorder.cancelRecording()

        // We can't access the private voiceFile directly, but we can verify that the cache directory
        // has no active .opus files that were left undeleted, or we can check the thread states.
        // Wait for thread join to finish inside cancelRecording
        
        // Let's list files in context.cacheDir and check if any voice_message_*.opus files exist
        val opusFiles = context.cacheDir.listFiles { _, name -> name.startsWith("voice_message_") && name.endsWith(".opus") } ?: emptyArray()
        assertTrue(opusFiles.isEmpty(), "Cancelled voice recording file should be deleted automatically")
    }

    @Test
    fun testDoubleStartFails() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recorder = VoiceRecorderImpl(context, Dispatchers.Unconfined)

        val firstStart = recorder.startRecording()
        assertTrue(firstStart, "First start should succeed")

        // Starting again while active should fail
        val secondStart = recorder.startRecording()
        assertFalse(secondStart, "Starting recording while already active should return false")

        // Clean up
        recorder.stopRecording()
        
        // Clean cache files
        val opusFiles = context.cacheDir.listFiles { _, name -> name.startsWith("voice_message_") && name.endsWith(".opus") } ?: emptyArray()
        opusFiles.forEach { it.delete() }
    }
}
