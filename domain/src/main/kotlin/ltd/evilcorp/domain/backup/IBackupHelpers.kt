package ltd.evilcorp.domain.backup

import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.FileTransfer
import ltd.evilcorp.domain.model.Message

interface IChatHistoryBackupHelper {
    fun serializeChatHistory(): List<Message>
    fun deserializeChatHistory(messages: List<Message>)
    fun serializeCallLog(): List<Message>
    fun deserializeCallLog(messages: List<Message>)
}

interface IContactsBackupHelper {
    fun serializeContacts(): List<Contact>
    fun deserializeContacts(contacts: List<Contact>)
}

interface IFileTransferBackupHelper {
    fun serializeFileTransfers(): List<FileTransfer>
    fun deserializeFileTransfers(transfers: List<FileTransfer>)
    fun setDestination(id: Int, destination: String)
}
