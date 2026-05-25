package ltd.evilcorp.domain.model

import ltd.evilcorp.core.model.Sender
import ltd.evilcorp.core.model.MessageType

data class DomainMessage(
    val publicKey: String,
    val message: String,
    val sender: Sender,
    val type: MessageType,
    var correlationId: Int,
    var timestamp: Long = 0,
    var id: Long = 0,
)
