package ltd.evilcorp.domain.features.contacts.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.chat.repository.IMessageRepository
import ltd.evilcorp.domain.core.network.ToxID
import ltd.evilcorp.domain.features.contacts.ContactManager
import ltd.evilcorp.domain.core.network.ITimeProvider

class AddContactUseCase @Inject constructor(
    private val contactManager: ContactManager,
    private val messageRepository: IMessageRepository,
    private val timeProvider: ITimeProvider,
) {
    suspend fun execute(toxId: ToxID, message: String) = withContext(Dispatchers.IO) {
        contactManager.add(toxId, message)
        messageRepository.add(
            Message(
                toxId.toPublicKey().string(),
                message,
                Sender.Sent,
                MessageType.Normal,
                0,
                timeProvider.getCurrentTimeMillis(),
            )
        )
    }
}
