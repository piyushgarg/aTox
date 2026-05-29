package ltd.evilcorp.domain.features.chat.usecase

import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.features.chat.ChatManager
import javax.inject.Inject

class ClearChatHistoryUseCase @Inject constructor(
    private val chatManager: ChatManager
) {
    suspend fun execute(publicKey: PublicKey) {
        chatManager.clearHistory(publicKey)
    }
}
