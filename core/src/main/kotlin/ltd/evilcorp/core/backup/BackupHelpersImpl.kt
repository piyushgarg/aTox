package ltd.evilcorp.core.backup

import javax.inject.Inject
import javax.inject.Singleton
import ltd.evilcorp.core.db.ContactDao
import ltd.evilcorp.core.db.FileTransferDao
import ltd.evilcorp.core.db.MessageDao
import ltd.evilcorp.core.db.entity.ContactEntity
import ltd.evilcorp.core.db.entity.FileTransferEntity
import ltd.evilcorp.core.db.entity.MessageEntity
import ltd.evilcorp.domain.backup.IChatHistoryBackupHelper
import ltd.evilcorp.domain.backup.IContactsBackupHelper
import ltd.evilcorp.domain.backup.IFileTransferBackupHelper
import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.FileTransfer
import ltd.evilcorp.domain.model.Message

@Singleton
class ChatHistoryBackupHelperImpl @Inject constructor(
    private val messageDao: MessageDao,
) : IChatHistoryBackupHelper {
    override fun serializeChatHistory(): List<Message> =
        messageDao.loadAllBlocking().map { it.toDomain() }

    override fun deserializeChatHistory(messages: List<Message>) {
        messageDao.saveAll(messages.map { MessageEntity.fromDomain(it) })
    }

    override fun serializeCallLog(): List<Message> =
        messageDao.loadAllBlocking()
            .filter { it.correlationId == Int.MIN_VALUE }
            .map { it.toDomain() }

    override fun deserializeCallLog(messages: List<Message>) {
        messageDao.saveAll(messages.map { MessageEntity.fromDomain(it) })
    }
}

@Singleton
class ContactsBackupHelperImpl @Inject constructor(
    private val contactDao: ContactDao,
) : IContactsBackupHelper {
    override fun serializeContacts(): List<Contact> =
        contactDao.loadAllBlocking().map { it.toDomain() }

    override fun deserializeContacts(contacts: List<Contact>) {
        contactDao.saveAll(contacts.map { ContactEntity.fromDomain(it) })
    }
}

@Singleton
class FileTransferBackupHelperImpl @Inject constructor(
    private val fileTransferDao: FileTransferDao,
) : IFileTransferBackupHelper {
    override fun serializeFileTransfers(): List<FileTransfer> =
        fileTransferDao.loadAllBlocking().map { it.toDomain() }

    override fun deserializeFileTransfers(transfers: List<FileTransfer>) {
        fileTransferDao.saveAll(transfers.map { FileTransferEntity.fromDomain(it) })
    }

    override fun setDestination(id: Int, destination: String) {
        fileTransferDao.setDestination(id, destination)
    }
}
