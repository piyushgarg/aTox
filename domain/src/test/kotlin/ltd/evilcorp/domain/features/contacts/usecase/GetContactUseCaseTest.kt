package ltd.evilcorp.domain.features.contacts.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.fakes.FakeContactRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetContactUseCaseTest {

    @Test
    fun `get contact use case returns contact when exists`() = runTest {
        // Arrange
        val repo = FakeContactRepository()
        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val contact = Contact(pk.string(), name = "Alice")
        repo.add(contact)
        val useCase = GetContactUseCase(repo)

        // Act
        val result = useCase.execute(pk).first()

        // Assert
        assertEquals("Alice", result?.name)
    }

    @Test
    fun `get contact use case returns null when not exists`() = runTest {
        // Arrange
        val repo = FakeContactRepository()
        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        val useCase = GetContactUseCase(repo)

        // Act
        val result = useCase.execute(pk).first()

        // Assert
        assertNull(result)
    }
}
