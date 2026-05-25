package ltd.evilcorp.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import ltd.evilcorp.core.model.Message
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.Sender
import ltd.evilcorp.core.repository.MessageRepository
import ltd.evilcorp.core.tox.ToxID
import ltd.evilcorp.domain.feature.ContactManager

class AddContactUseCase @Inject constructor(
    private val contactManager: ContactManager,
    private val messageRepository: MessageRepository,
) {
    suspend fun execute(toxId: ToxID, message: String) = withContext(Dispatchers.IO) {
        contactManager.add(toxId, message).join()
        messageRepository.add(
            Message(
                toxId.toPublicKey().string(),
                message,
                Sender.Sent,
                MessageType.Normal,
                0,
                Date().time,
            )
        )
    }
}
