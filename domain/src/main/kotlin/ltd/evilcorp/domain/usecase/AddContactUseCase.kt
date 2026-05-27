package ltd.evilcorp.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import ltd.evilcorp.domain.model.Message
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.domain.model.Sender
import ltd.evilcorp.domain.repository.IMessageRepository
import ltd.evilcorp.domain.tox.ToxID
import ltd.evilcorp.domain.feature.ContactManager

class AddContactUseCase @Inject constructor(
    private val contactManager: ContactManager,
    private val messageRepository: IMessageRepository,
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
