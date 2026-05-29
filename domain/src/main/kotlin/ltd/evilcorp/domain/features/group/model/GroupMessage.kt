package ltd.evilcorp.domain.features.group.model
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.chat.model.MessageType

data class GroupMessage(
    val groupChatId: String,
    val peerId: Int,
    val senderName: String,
    val message: String,
    val sender: Sender,
    val type: MessageType,
    var correlationId: Int,
    var timestamp: Long = 0,
) {
    var id: Long = 0

    val colorIndex: Int
        get() = (senderName.hashCode().let { if (it < 0) -it else it } % 8)
}

