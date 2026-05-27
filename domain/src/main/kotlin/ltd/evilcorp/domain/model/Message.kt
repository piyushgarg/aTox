package ltd.evilcorp.domain.model

enum class Sender {
    Sent,
    Received,
}

enum class MessageType {
    Normal,
    Action,
    FileTransfer,
    GroupEvent,
}

@Stable
data class Message(
    val publicKey: String,
    val message: String,
    val sender: Sender,
    val type: MessageType,
    var correlationId: Int,
    var timestamp: Long = 0,
) {
    var id: Long = 0
}
