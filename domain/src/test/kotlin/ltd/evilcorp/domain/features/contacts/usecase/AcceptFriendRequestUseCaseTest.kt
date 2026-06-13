package ltd.evilcorp.domain.features.contacts.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.features.contacts.FriendRequestManager
import ltd.evilcorp.domain.features.contacts.model.FriendRequest
import ltd.evilcorp.domain.fakes.FakeContactRepository
import ltd.evilcorp.domain.fakes.FakeFriendRequestRepository
import ltd.evilcorp.domain.fakes.FakeMessageRepository
import ltd.evilcorp.domain.fakes.FakeToxProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AcceptFriendRequestUseCaseTest {

    @Test
    fun `accept friend request adds contact and message, deletes request`() = runTest {
        // Arrange
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val friendRequestRepo = FakeFriendRequestRepository()
        val messageRepo = FakeMessageRepository()
        val toxProfile = FakeToxProfile()

        val manager = FriendRequestManager(scope, contactRepo, friendRequestRepo, messageRepo, toxProfile)
        val useCase = AcceptFriendRequestUseCase(manager)

        val req = FriendRequest("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C", "Let's chat!")
        friendRequestRepo.add(req)

        // Act
        useCase.execute(req)

        // Assert
        // 1. Check contact added
        assertTrue(contactRepo.exists(req.publicKey))
        val contact = contactRepo.get(req.publicKey).first()
        assertEquals(req.publicKey, contact?.publicKey)

        // 2. Check message added
        val msgs = messageRepo.get(req.publicKey).first()
        assertEquals(1, msgs.size)
        assertEquals("Let's chat!", msgs[0].message)

        // 3. Check request deleted
        assertEquals(0, friendRequestRepo.count())
    }
}
