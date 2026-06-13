package ltd.evilcorp.atox.appearance

import android.app.LocaleManager
import android.content.Context
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.features.settings.model.UserSettings
import ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppearanceManagerTest {

    private val mockContext = mockk<Context>(relaxed = true)
    private val mockRepository = mockk<IUserSettingsRepository>(relaxed = true)
    private val settingsFlow = MutableStateFlow(UserSettings())

    init {
        every { mockRepository.settings } returns settingsFlow
    }

    private suspend fun awaitCondition(timeoutMs: Long = 1000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw AssertionError("Condition not met within $timeoutMs ms")
            }
            kotlinx.coroutines.delay(10)
        }
    }

    @Test
    fun testThemeModeUpdate() = runTest {
        val manager = AppearanceManager(mockContext, mockRepository)
        
        settingsFlow.value = UserSettings(themeMode = 1)
        awaitCondition { manager.appearance.value.themeMode == 1 }
        assertEquals(1, manager.appearance.value.themeMode)
        
        manager.updateThemeMode(2)
        runCurrent()
        coVerify { mockRepository.updateThemeMode(2) }
    }

    @Test
    fun testDynamicColorEnabledUpdate() = runTest {
        val manager = AppearanceManager(mockContext, mockRepository)
        
        settingsFlow.value = UserSettings(dynamicColorEnabled = false)
        awaitCondition { !manager.appearance.value.dynamicColorEnabled }
        assertEquals(false, manager.appearance.value.dynamicColorEnabled)
        
        manager.updateDynamicColorEnabled(true)
        runCurrent()
        coVerify { mockRepository.updateDynamicColorEnabled(true) }
    }

    @Test
    fun testAccentColorSeedUpdate() = runTest {
        val manager = AppearanceManager(mockContext, mockRepository)
        
        settingsFlow.value = UserSettings(accentColorSeed = 123)
        awaitCondition { manager.appearance.value.accentColorSeed == 123 }
        assertEquals(123, manager.appearance.value.accentColorSeed)
        
        manager.updateAccentColorSeed(456)
        runCurrent()
        coVerify { mockRepository.updateAccentColorSeed(456) }
    }

    @Test
    fun testLocaleTagUpdate() = runTest {
        val mockLocaleManager = mockk<LocaleManager>(relaxed = true)
        every { mockContext.getSystemService(LocaleManager::class.java) } returns mockLocaleManager
        
        val manager = AppearanceManager(mockContext, mockRepository)
        
        settingsFlow.value = UserSettings(localeTag = "en")
        awaitCondition { manager.appearance.value.localeTag == "en" }
        assertEquals("en", manager.appearance.value.localeTag)
        
        manager.updateLocaleTag("ru")
        runCurrent()
        coVerify { mockRepository.updateLocaleTag("ru") }
        verify { mockLocaleManager.applicationLocales = any() }
    }
}
