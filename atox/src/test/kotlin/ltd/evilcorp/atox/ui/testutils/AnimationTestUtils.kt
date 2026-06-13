package ltd.evilcorp.atox.ui.testutils

import androidx.compose.ui.test.junit4.ComposeContentTestRule

/**
 * Extension functions to aid in testing Compose animations using mainClock.
 */
object AnimationTestUtils {

    /**
     * Advances the main clock by the given amount of milliseconds and waits for idle.
     * Useful for checking intermediate states in animations.
     */
    fun ComposeContentTestRule.advanceTimeByAndIdle(millis: Long) {
        mainClock.advanceTimeBy(millis)
        waitForIdle()
    }
}
