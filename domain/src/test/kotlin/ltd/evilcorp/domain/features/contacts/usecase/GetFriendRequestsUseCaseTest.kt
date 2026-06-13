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

class GetFriendRequestsUseCaseTest {

    @Test
    fun `get friend requests use case returns requests`() = runTest {
        // Arrange
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val contactRepo = FakeContactRepository()
        val friendRequestRepo = FakeFriendRequestRepository()
        val messageRepo = FakeMessageRepository()
        val toxProfile = FakeToxProfile()

        val manager = FriendRequestManager(scope, contactRepo, friendRequestRepo, messageRepo, toxProfile)
        
        val req = FriendRequest("pk1", "Hello! Let's be friends.")
        friendRequestRepo.add(req)
        
        val useCase = GetFriendRequestsUseCase(manager)

        // Act
        val result = useCase.execute().first()

        // Assert
        assertEquals(1, result.size)
        assertEquals("pk1", result[0].publicKey)
        assertEquals("Hello! Let's be friends.", result[0].message)
    }
}
