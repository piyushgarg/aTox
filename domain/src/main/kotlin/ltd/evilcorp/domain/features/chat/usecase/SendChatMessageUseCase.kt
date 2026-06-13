package ltd.evilcorp.domain.features.chat.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ltd.evilcorp.domain.core.di.IoDispatcher
import ltd.evilcorp.domain.features.chat.ChatManager
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.core.model.PublicKey
import javax.inject.Inject

class SendChatMessageUseCase @Inject constructor(
    private val chatManager: ChatManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun execute(
        publicKey: PublicKey,
        message: String,
        type: MessageType,
        replyToMessageId: Int? = null
    ) = withContext(ioDispatcher) {
        val finalMessage = if (replyToMessageId != null) {
            "[reply:$replyToMessageId] $message"
        } else {
            message
        }
        chatManager.sendMessage(publicKey, finalMessage, type)
    }
}

