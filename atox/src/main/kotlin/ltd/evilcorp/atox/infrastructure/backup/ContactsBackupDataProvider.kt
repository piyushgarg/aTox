package ltd.evilcorp.atox.infrastructure.backup

import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import ltd.evilcorp.atox.R
import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider
import ltd.evilcorp.domain.features.backup.repository.IContactsBackupHelper
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import ltd.evilcorp.domain.features.contacts.model.UserStatus

class ContactsBackupDataProvider @Inject constructor(
    private val helper: IContactsBackupHelper,
) : IBackupDataProvider {
    override val id: String = "contacts"
    override val displayNameRes: Int = R.string.backup_module_contacts
    override val descriptionRes: Int = R.string.backup_module_contacts_description

    override suspend fun serialize(): ByteArray {
        val container = ContactsBackupContainer(
            contacts = helper.serializeContacts().map { contact ->
                ContactBackupPayload(
                    publicKey = contact.publicKey,
                    name = contact.name,
                    statusMessage = contact.statusMessage,
                    lastMessage = contact.lastMessage,
                    status = contact.status.name,
                    connectionStatus = contact.connectionStatus.name,
                    avatarUri = contact.avatarUri,
                    hasUnreadMessages = contact.hasUnreadMessages,
                    draftMessage = contact.draftMessage,
                    lastOnline = contact.lastOnline
                )
            }
        )
        return Json.encodeToString(container).encodeToByteArray()
    }

    override suspend fun deserialize(data: ByteArray) {
        val container = Json.decodeFromString<ContactsBackupContainer>(data.decodeToString())
        val restored = container.contacts.map { item ->
            Contact(
                publicKey = item.publicKey,
                name = item.name ?: "",
                statusMessage = item.statusMessage ?: "...",
                lastMessage = item.lastMessage,
                status = try { enumValueOf(item.status ?: UserStatus.None.name) } catch (e: Exception) { UserStatus.None },
                connectionStatus = try { enumValueOf(item.connectionStatus ?: ConnectionStatus.None.name) } catch (e: Exception) { ConnectionStatus.None },
                avatarUri = item.avatarUri ?: "",
                hasUnreadMessages = item.hasUnreadMessages,
                draftMessage = item.draftMessage ?: "",
                lastOnline = item.lastOnline
            )
        }
        helper.deserializeContacts(restored)
    }
}

@Serializable
private data class ContactBackupPayload(
    val publicKey: String,
    val name: String? = null,
    val statusMessage: String? = null,
    val lastMessage: Long = 0L,
    val status: String? = null,
    val connectionStatus: String? = null,
    val avatarUri: String? = null,
    val hasUnreadMessages: Boolean = false,
    val draftMessage: String? = null,
    val lastOnline: Long = 0L
)

@Serializable
private data class ContactsBackupContainer(
    val contacts: List<ContactBackupPayload>
)
