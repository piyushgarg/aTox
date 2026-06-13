package ltd.evilcorp.core.tox

import androidx.test.ext.junit.runners.AndroidJUnit4
import ltd.evilcorp.core.tox.save.ToxSaveTesterImpl
import ltd.evilcorp.core.tox.save.testToxSave
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import ltd.evilcorp.domain.features.settings.model.ProxyStatus
import ltd.evilcorp.domain.features.settings.model.ProxyType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ToxSaveTesterImplTest {

    private lateinit var saveTester: ToxSaveTesterImpl

    @Before
    fun setUp() {
        saveTester = ToxSaveTesterImpl()
    }

    @Test
    fun testProxy_goodStatus_whenNoProxy() {
        val status = saveTester.testProxy(
            udpEnabled = false,
            proxyType = ProxyType.None,
            proxyAddress = "",
            proxyPort = 0
        )
        assertEquals(ProxyStatus.Good, status)
    }

    @Test
    fun testToxSave_badFormat_onInvalidBytes() {
        val options = SaveOptions(
            saveData = byteArrayOf(1, 2, 3, 4, 5),
            udpEnabled = false,
            proxyType = ProxyType.None,
            proxyAddress = "",
            proxyPort = 0
        )
        
        val status = testToxSave(options, null)
        assertEquals(ToxSaveStatus.BadFormat, status)
    }

    @Test
    fun testToxSave_null_whenNoSaveData() {
        val options = SaveOptions(
            saveData = null,
            udpEnabled = false,
            proxyType = ProxyType.None,
            proxyAddress = "",
            proxyPort = 0
        )
        
        val status = testToxSave(options, null)
        // With null data, toxNew returns 0L, yielding BadFormat
        assertEquals(ToxSaveStatus.BadFormat, status)
    }
}
