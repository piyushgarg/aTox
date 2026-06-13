package ltd.evilcorp.domain.features.group.model
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.chat.model.MessageType

import ltd.evilcorp.domain.core.model.Stable

@Stable
data class GroupMessage(
    val groupChatId: String,
    val peerId: Int,
    val senderName: String,
    val message: String,
    val sender: Sender,
    val type: MessageType,
    val correlationId: Int,
    val timestamp: Long = 0,
    val id: Long = 0,
) {

    val colorIndex: Int
        get() = (senderName.hashCode().let { if (it < 0) -it else it } % 8)
}

