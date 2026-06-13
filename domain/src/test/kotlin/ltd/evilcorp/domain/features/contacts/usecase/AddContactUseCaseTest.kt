package ltd.evilcorp.domain.features.contacts.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.network.ITimeProvider
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.fakes.FakeContactRepository
import ltd.evilcorp.domain.fakes.FakeMessageRepository
import ltd.evilcorp.domain.fakes.FakeToxProfile
import ltd.evilcorp.domain.features.contacts.ContactManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddContactUseCaseTest {

    @Test
    fun `executing add contact updates repositories and tox profile`() = runTest {
        // Arrange
        val fakeContactRepo = FakeContactRepository()
        val fakeToxProfile = FakeToxProfile()
        val contactManager = ContactManager(fakeContactRepo, fakeToxProfile, Dispatchers.Unconfined)
        val fakeMessageRepo = FakeMessageRepository()
        
        val fixedTime = 1234567890L
        val fakeTimeProvider = object : ITimeProvider {
            override fun getCurrentTimeMillis(): Long = fixedTime
        }

        val useCase = AddContactUseCase(contactManager, fakeMessageRepo, fakeTimeProvider, Dispatchers.Unconfined)
        val toxId = ToxID("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C4ACEE797596D")
        val message = "Hello from testing!"

        // Act
        useCase.execute(toxId, message)

        // Assert
        // 1. Verify JNI call
        assertEquals(1, fakeToxProfile.addedContacts.size)
        assertEquals(toxId, fakeToxProfile.addedContacts[0].first)
        assertEquals(message, fakeToxProfile.addedContacts[0].second)

        // 2. Verify ContactRepository updates
        val contactPk = toxId.toPublicKey().string()
        assertTrue(fakeContactRepo.exists(contactPk))
        val contact = fakeContactRepo.get(contactPk).first()
        assertEquals(contactPk, contact?.publicKey)

        // 3. Verify MessageRepository updates
        val allMsgs = fakeMessageRepo.get(contactPk).first()
        assertEquals(1, allMsgs.size)
        assertEquals(message, allMsgs[0].message)
        assertEquals(fixedTime, allMsgs[0].timestamp)
    }
}
