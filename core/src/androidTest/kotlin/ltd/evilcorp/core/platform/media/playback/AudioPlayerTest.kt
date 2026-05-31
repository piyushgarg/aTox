// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.platform.media.playback

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class AudioPlayerTest {

    @Test
    fun testAudioPlayerLifecycle() {
        val sampleRate = 48000
        val channels = 1
        val player = AudioPlayer(sampleRate, channels)
        
        assertNotNull(player)
        
        // Start playback
        player.start()
        
        // Feed quiet buffer (silence)
        val data = ShortArray(960)
        player.buffer(data)
        
        // Stop playback
        player.stop()
        
        // Release resources
        player.release()
    }
}
