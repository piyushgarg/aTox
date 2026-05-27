package ltd.evilcorp.domain.model

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
}
