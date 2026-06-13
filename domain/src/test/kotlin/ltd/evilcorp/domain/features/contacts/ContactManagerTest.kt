package ltd.evilcorp.domain.features.contacts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.fakes.FakeContactRepository
import ltd.evilcorp.domain.fakes.FakeTox
import ltd.evilcorp.domain.features.contacts.model.Contact
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContactManagerTest {

    @Test
    fun `add contact updates repo and tox`() = runTest {
        // Arrange
        val repo = FakeContactRepository()
        val tox = FakeTox()
        val manager = ContactManager(repo, tox, Dispatchers.Unconfined)
        val toxId = ToxID("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C4ACEE797596D")

        // Act
        manager.add(toxId, "Hello")

        // Assert
        assertTrue(repo.exists(toxId.toPublicKey().string()))
        assertEquals(1, tox.addedContacts.size)
        assertEquals(toxId, tox.addedContacts[0].first)
    }

    @Test
    fun `delete contact updates repo and tox`() = runTest {
        // Arrange
        val repo = FakeContactRepository()
        val tox = FakeTox()
        val manager = ContactManager(repo, tox, Dispatchers.Unconfined)
        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        repo.add(Contact(pk.string(), name = "Alice"))

        // Act
        manager.delete(pk)

        // Assert
        assertFalse(repo.exists(pk.string()))
        assertEquals(1, tox.deletedContacts.size)
        assertEquals(pk, tox.deletedContacts[0])
    }

    @Test
    fun `set draft updates repo`() = runTest {
        // Arrange
        val repo = FakeContactRepository()
        val tox = FakeTox()
        val manager = ContactManager(repo, tox, Dispatchers.Unconfined)
        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        repo.add(Contact(pk.string()))

        // Act
        manager.setDraft(pk, "Draft msg")

        // Assert
        val contact = repo.get(pk.string()).first()
        assertEquals("Draft msg", contact?.draftMessage)
    }
}
