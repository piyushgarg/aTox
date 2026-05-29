package ltd.evilcorp.domain.features.chat.usecase

import ltd.evilcorp.domain.features.chat.ChatManager
import javax.inject.Inject

class DeleteChatMessageUseCase @Inject constructor(
    private val chatManager: ChatManager
) {
    suspend fun execute(id: Long) {
        chatManager.deleteMessage(id)
    }
}
