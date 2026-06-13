package ltd.evilcorp.domain.features.chat.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.fakes.FakeContactRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class SetChatDraftUseCaseTest {

    @Test
    fun `execute updates draft message in repo`() = runTest {
        // Arrange
        val repo = FakeContactRepository()
        val pk = PublicKey("3982B009845B210C5A8904B7F540287A424DE029BC1A25C01E022944AB28FC3C")
        repo.add(Contact(pk.string()))
        val useCase = SetChatDraftUseCase(repo)

        // Act
        useCase.execute(pk, "Typed draft message")

        // Assert
        val contact = repo.get(pk.string()).first()
        assertEquals("Typed draft message", contact?.draftMessage)
    }
}
