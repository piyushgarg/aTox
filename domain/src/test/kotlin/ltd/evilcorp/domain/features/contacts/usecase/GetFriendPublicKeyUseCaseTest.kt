package ltd.evilcorp.domain.features.contacts.usecase

import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.fakes.FakeTox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetFriendPublicKeyUseCaseTest {

    @Test
    fun `get friend public key returns correct public key`() {
        // Arrange
        val expectedPk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val fakeTox = object : FakeTox() {
            override fun getFriendPublicKey(friendNumber: Int): PublicKey? {
                return if (friendNumber == 42) expectedPk else null
            }
        }
        val useCase = GetFriendPublicKeyUseCase(fakeTox)

        // Act & Assert
        assertEquals(expectedPk, useCase.execute(42))
        assertNull(useCase.execute(7))
    }
}
