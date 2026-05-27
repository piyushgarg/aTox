package ltd.evilcorp.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.feature.ChatManager
import ltd.evilcorp.domain.model.MessageType
import ltd.evilcorp.domain.model.PublicKey
import javax.inject.Inject

class SendChatMessageUseCase @Inject constructor(
    private val chatManager: ChatManager
) {
    suspend fun execute(publicKey: PublicKey, message: String, type: MessageType) = withContext(Dispatchers.IO) {
        chatManager.sendMessage(publicKey, message, type)
    }
}
