package ltd.evilcorp.domain.model

import ltd.evilcorp.core.model.Contact
import ltd.evilcorp.core.model.User
import ltd.evilcorp.core.model.Message

fun Contact.toDomain(): DomainContact = DomainContact(
    publicKey = this.publicKey,
    name = this.name,
    statusMessage = this.statusMessage,
    lastMessage = this.lastMessage,
    status = this.status,
    connectionStatus = this.connectionStatus,
    typing = this.typing,
    avatarUri = this.avatarUri,
    hasUnreadMessages = this.hasUnreadMessages,
    draftMessage = this.draftMessage,
    lastOnline = this.lastOnline
)

fun DomainContact.toDb(): Contact = Contact(
    publicKey = this.publicKey,
    name = this.name,
    statusMessage = this.statusMessage,
    lastMessage = this.lastMessage,
    status = this.status,
    connectionStatus = this.connectionStatus,
    typing = this.typing,
    avatarUri = this.avatarUri,
    hasUnreadMessages = this.hasUnreadMessages,
    draftMessage = this.draftMessage,
    lastOnline = this.lastOnline
)

fun User.toDomain(): DomainUser = DomainUser(
    publicKey = this.publicKey,
    name = this.name,
    statusMessage = this.statusMessage,
    status = this.status,
    connectionStatus = this.connectionStatus,
    password = this.password
)

fun DomainUser.toDb(): User = User(
    publicKey = this.publicKey,
    name = this.name,
    statusMessage = this.statusMessage,
    status = this.status,
    connectionStatus = this.connectionStatus,
    password = this.password
)

fun Message.toDomain(): DomainMessage = DomainMessage(
    publicKey = this.publicKey,
    message = this.message,
    sender = this.sender,
    type = this.type,
    correlationId = this.correlationId,
    timestamp = this.timestamp,
    id = this.id
)

fun DomainMessage.toDb(): Message = Message(
    publicKey = this.publicKey,
    message = this.message,
    sender = this.sender,
    type = this.type,
    correlationId = this.correlationId,
    timestamp = this.timestamp
).apply {
    this.id = this.id
}
