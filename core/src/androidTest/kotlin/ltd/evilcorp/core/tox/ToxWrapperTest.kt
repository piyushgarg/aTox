package ltd.evilcorp.core.tox

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ltd.evilcorp.core.tox.listener.ToxAvEventListener
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.core.tox.runtime.ToxWrapper
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.core.network.save.SaveOptions
import ltd.evilcorp.domain.features.contacts.model.UserStatus
import ltd.evilcorp.domain.features.settings.model.ProxyType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ToxWrapperTest {

    private lateinit var toxWrapper: ToxWrapper

    @Before
    fun setUp() {
        val options = SaveOptions(null, false, ProxyType.None, "", 0)
        toxWrapper = ToxWrapper(
            ToxEventListener(),
            ToxAvEventListener(),
            options
        )
    }

    @After
    fun tearDown() {
        toxWrapper.close()
    }

    @Test
    fun testSelfProfileProperties() {
        // Test names
        assertEquals("", toxWrapper.getName())
        toxWrapper.setName("Test Robot")
        assertEquals("Test Robot", toxWrapper.getName())

        // Test status message
        assertEquals("", toxWrapper.getStatusMessage())
        toxWrapper.setStatusMessage("Busy typing")
        assertEquals("Busy typing", toxWrapper.getStatusMessage())

        // Test user status
        assertEquals(UserStatus.None, toxWrapper.getStatus())
        toxWrapper.setStatus(UserStatus.Busy)
        assertEquals(UserStatus.Busy, toxWrapper.getStatus())
    }

    @Test
    fun testNospamAndSelfKeys() {
        // Nospam is a random 32-bit int initially or 0. We can set and get it.
        val nospam = 12345678
        toxWrapper.setNospam(nospam)
        assertEquals(nospam, toxWrapper.getNospam())

        // Public key and Tox ID
        val selfPk = toxWrapper.getPublicKey()
        val selfToxId = toxWrapper.getToxId()
        assertNotNull(selfPk)
        assertNotNull(selfToxId)
        assertTrue(selfToxId.string().startsWith(selfPk.string()))

        // Secret key
        val secretKey = toxWrapper.selfGetSecretKey()
        assertNotNull(secretKey)
        assertEquals(32, secretKey.size)

        // DHT / Port getters
        assertTrue(toxWrapper.selfGetUdpPort() >= 0)
        assertTrue(toxWrapper.selfGetTcpPort() >= 0)
        assertNotNull(toxWrapper.selfGetDhtId())
    }

    @Test
    fun testContactsManagement() {
        // Initial contacts
        assertTrue(toxWrapper.getContacts().isEmpty())

        val randomPkBytes = ByteArray(32) { it.toByte() }
        val contactPk = PublicKey.fromBytes(randomPkBytes)

        // Add contact
        val friendNumber = toxWrapper.addFriendNoRequest(contactPk)
        assertTrue(friendNumber >= 0)

        // Retrieve and check list
        val contacts = toxWrapper.getContacts()
        assertEquals(1, contacts.size)
        assertEquals(contactPk, contacts[0].first)

        // Typing state (default is false)
        assertFalse(toxWrapper.friendGetTyping(contactPk))
        toxWrapper.setTyping(contactPk, true)

        // Delete contact
        toxWrapper.deleteContact(contactPk)
        assertTrue(toxWrapper.getContacts().isEmpty())
    }

    @Test
    fun testSaveDataRetrieval() {
        val saveData = toxWrapper.getSaveData()
        assertNotNull(saveData)
        assertTrue(saveData.isNotEmpty())
    }
}
