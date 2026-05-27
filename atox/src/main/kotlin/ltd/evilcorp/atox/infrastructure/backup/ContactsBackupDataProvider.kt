package ltd.evilcorp.atox.infrastructure.backup

import javax.inject.Inject
import ltd.evilcorp.atox.R
import ltd.evilcorp.core.db.ContactDao
import ltd.evilcorp.domain.model.ConnectionStatus
import ltd.evilcorp.domain.model.Contact
import ltd.evilcorp.domain.model.UserStatus
import ltd.evilcorp.domain.backup.BackupDataProvider
import org.json.JSONArray
import org.json.JSONObject

class ContactsBackupDataProvider @Inject constructor(
    private val contactDao: ContactDao,
) : BackupDataProvider {
    override val id: String = "contacts"
    override val displayNameRes: Int = R.string.backup_module_contacts
    override val descriptionRes: Int = R.string.backup_module_contacts_description

    override fun serialize(): ByteArray {
        val contacts = JSONArray()
        contactDao.loadAllBlocking().forEach { contact ->
            contacts.put(JSONObject().apply {
                put("publicKey", contact.publicKey)
                put("name", contact.name)
                put("statusMessage", contact.statusMessage)
                put("lastMessage", contact.lastMessage)
                put("status", contact.status.name)
                put("connectionStatus", contact.connectionStatus.name)
                put("avatarUri", contact.avatarUri)
                put("hasUnreadMessages", contact.hasUnreadMessages)
                put("draftMessage", contact.draftMessage)
                put("lastOnline", contact.lastOnline)
            })
        }
        return JSONObject().put("contacts", contacts).toString().encodeToByteArray()
    }

    override fun deserialize(data: ByteArray) {
        val contacts = JSONObject(data.decodeToString()).getJSONArray("contacts")
        val restored = buildList {
            for (index in 0 until contacts.length()) {
                val item = contacts.getJSONObject(index)
                add(Contact(
                    publicKey = item.getString("publicKey"),
                    name = item.optString("name"),
                    statusMessage = item.optString("statusMessage", "..."),
                    lastMessage = item.optLong("lastMessage"),
                    status = enumValueOf(item.optString("status", UserStatus.None.name)),
                    connectionStatus = enumValueOf(item.optString("connectionStatus", ConnectionStatus.None.name)),
                    avatarUri = item.optString("avatarUri"),
                    hasUnreadMessages = item.optBoolean("hasUnreadMessages"),
                    draftMessage = item.optString("draftMessage"),
                    lastOnline = item.optLong("lastOnline"),
                ))
            }
        }
        contactDao.saveAll(restored)
    }
}
