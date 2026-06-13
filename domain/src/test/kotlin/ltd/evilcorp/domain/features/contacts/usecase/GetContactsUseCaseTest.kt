package ltd.evilcorp.domain.features.contacts.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.fakes.FakeContactRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetContactsUseCaseTest {

    @Test
    fun `get contacts use case returns all contacts`() = runTest {
        // Arrange
        val repo = FakeContactRepository()
        val c1 = Contact("pk1", name = "Alice")
        val c2 = Contact("pk2", name = "Bob")
        repo.add(c1)
        repo.add(c2)
        val useCase = GetContactsUseCase(repo)

        // Act
        val result = useCase.execute().first()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Alice" })
        assertTrue(result.any { it.name == "Bob" })
    }

    @Test
    fun `get contacts use case returns empty list when no contacts`() = runTest {
        // Arrange
        val repo = FakeContactRepository()
        val useCase = GetContactsUseCase(repo)

        // Act
        val result = useCase.execute().first()

        // Assert
        assertTrue(result.isEmpty())
    }
}
