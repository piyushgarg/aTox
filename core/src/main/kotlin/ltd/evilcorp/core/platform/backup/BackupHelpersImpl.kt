package ltd.evilcorp.core.platform.backup

import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.core.db.dao.ContactDao
import ltd.evilcorp.core.db.dao.FileTransferDao
import ltd.evilcorp.core.db.dao.MessageDao
import ltd.evilcorp.core.db.entity.ContactEntity
import ltd.evilcorp.core.db.entity.FileTransferEntity
import ltd.evilcorp.core.db.entity.MessageEntity
import ltd.evilcorp.domain.features.backup.repository.IChatHistoryBackupHelper
import ltd.evilcorp.domain.features.backup.repository.IContactsBackupHelper
import ltd.evilcorp.domain.features.backup.repository.IFileTransferBackupHelper
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.transfer.model.FileTransfer
import ltd.evilcorp.domain.features.chat.model.Message

@Singleton
class ChatHistoryBackupHelperImpl @Inject constructor(
    private val messageDao: MessageDao,
) : IChatHistoryBackupHelper {
    override suspend fun serializeChatHistory(): List<Message> =
        messageDao.loadAll().map { it.toDomain() }

    override suspend fun serializeChatHistoryPaged(limit: Int, offset: Int): List<Message> =
        messageDao.loadPaged(limit, offset).map { it.toDomain() }

    override suspend fun deserializeChatHistory(messages: List<Message>) {
        messageDao.saveAll(messages.map { MessageEntity.fromDomain(it) })
    }

    override suspend fun serializeCallLog(): List<Message> =
        messageDao.loadAll()
            .filter { it.correlationId == Int.MIN_VALUE }
            .map { it.toDomain() }

    override suspend fun serializeCallLogPaged(limit: Int, offset: Int): List<Message> =
        messageDao.loadCallLogPaged(limit, offset).map { it.toDomain() }

    override suspend fun deserializeCallLog(messages: List<Message>) {
        messageDao.saveAll(messages.map { MessageEntity.fromDomain(it) })
    }
}

@Singleton
class ContactsBackupHelperImpl @Inject constructor(
    private val contactDao: ContactDao,
) : IContactsBackupHelper {
    override suspend fun serializeContacts(): List<Contact> =
        contactDao.loadAllBlocking().map { it.toDomain() }

    override suspend fun deserializeContacts(contacts: List<Contact>) {
        contactDao.saveAll(contacts.map { ContactEntity.fromDomain(it) })
    }
}

@Singleton
class FileTransferBackupHelperImpl @Inject constructor(
    private val fileTransferDao: FileTransferDao,
) : IFileTransferBackupHelper {
    override suspend fun serializeFileTransfers(): List<FileTransfer> =
        fileTransferDao.loadAll().map { it.toDomain() }

    override suspend fun deserializeFileTransfers(transfers: List<FileTransfer>) {
        fileTransferDao.saveAll(transfers.map { FileTransferEntity.fromDomain(it) })
    }

    override suspend fun setDestination(id: Int, destination: String) {
        fileTransferDao.setDestination(id, destination)
    }
}
