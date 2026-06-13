package ltd.evilcorp.domain.features.backup.repository

import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.chat.model.Message

interface IChatHistoryBackupHelper {
    suspend fun serializeChatHistory(): List<Message>
    suspend fun serializeChatHistoryPaged(limit: Int, offset: Int): List<Message>
    suspend fun deserializeChatHistory(messages: List<Message>)
    suspend fun serializeCallLog(): List<Message>
    suspend fun serializeCallLogPaged(limit: Int, offset: Int): List<Message>
    suspend fun deserializeCallLog(messages: List<Message>)
}

interface IContactsBackupHelper {
    suspend fun serializeContacts(): List<Contact>
    suspend fun deserializeContacts(contacts: List<Contact>)
}

interface IFileTransferBackupHelper {
    suspend fun serializeFileTransfers(): List<FileTransfer>
    suspend fun deserializeFileTransfers(transfers: List<FileTransfer>)
    suspend fun setDestination(id: Int, destination: String)
}
