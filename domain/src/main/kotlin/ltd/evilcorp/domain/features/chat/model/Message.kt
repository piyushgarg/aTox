package ltd.evilcorp.domain.features.chat.model

import ltd.evilcorp.domain.core.model.Stable

enum class Sender(val id: Int) {
    Sent(0),
    Received(1);

    companion object {
        fun fromId(id: Int): Sender = entries.find { it.id == id } ?: Sent
    }
}

enum class MessageType(val id: Int) {
    Normal(0),
    Action(1),
    FileTransfer(2),
    GroupEvent(3);

    companion object {
        fun fromId(id: Int): MessageType = entries.find { it.id == id } ?: Normal
    }
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
