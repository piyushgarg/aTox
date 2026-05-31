package ltd.evilcorp.core.platform.system

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SystemTimeProviderImplTest {

    private lateinit var timeProvider: SystemTimeProviderImpl

    @BeforeTest
    fun setUp() {
        timeProvider = SystemTimeProviderImpl()
    }

    @Test
    fun testGetCurrentTimeMillis_returnsRecentTime() {
        val start = System.currentTimeMillis()
        val providerTime = timeProvider.getCurrentTimeMillis()
        val end = System.currentTimeMillis()

        assertTrue(providerTime in start..end)
    }
}
