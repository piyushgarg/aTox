package ltd.evilcorp.domain.features.contacts.usecase

import kotlinx.coroutines.flow.Flow
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.repository.IContactRepository
import javax.inject.Inject

/**
 * Use case to retrieve the list stream of all active user contacts.
 */
class GetContactsUseCase @Inject constructor(
    private val contactRepository: IContactRepository,
) {
    fun execute(): Flow<List<Contact>> {
        return contactRepository.getAll()
    }
}
