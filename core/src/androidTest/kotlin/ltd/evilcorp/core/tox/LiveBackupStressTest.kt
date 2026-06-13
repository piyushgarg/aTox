// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.tox

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.Database
import ltd.evilcorp.core.platform.JvmPlatformServices
import ltd.evilcorp.core.repository.ContactRepositoryImpl
import ltd.evilcorp.core.repository.MessageRepositoryImpl
import ltd.evilcorp.domain.features.backup.repository.IBackupDataProvider
import ltd.evilcorp.domain.features.backup.usecase.ExportBackupUseCase
import ltd.evilcorp.domain.features.backup.usecase.ImportBackupUseCase
import ltd.evilcorp.domain.features.chat.model.Message
import ltd.evilcorp.domain.features.chat.model.MessageType
import ltd.evilcorp.domain.features.chat.model.Sender
import ltd.evilcorp.domain.features.contacts.model.ConnectionStatus
import ltd.evilcorp.domain.features.contacts.model.Contact
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LiveBackupStressTest {

    private class ContactsBackupProvider(
        private val contactRepository: ContactRepositoryImpl
    ) : IBackupDataProvider {
        override val id: String = "contacts"
        override val displayNameRes: Int = 0
        override val descriptionRes: Int = 0

        override suspend fun serialize(outputStream: java.io.OutputStream) {
            val contacts = contactRepository.getAll().first()
            val sb = StringBuilder()
            contacts.forEach { c ->
                sb.append(c.publicKey).append("|")
                    .append(c.name).append("|")
                    .append(c.connectionStatus.name).append("\n")
            }
            outputStream.write(sb.toString().encodeToByteArray())
        }

        override suspend fun deserialize(data: ByteArray) {
            val text = String(data)
            if (text.isBlank()) return
            text.split("\n").forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = line.split("|")
                if (parts.size >= 3) {
                    val contact = Contact(
                        publicKey = parts[0],
                        name = parts[1],
                        connectionStatus = ConnectionStatus.valueOf(parts[2])
                    )
                    contactRepository.add(contact)
                }
            }
        }
    }

    private class MessagesBackupProvider(
        private val messageRepository: MessageRepositoryImpl
    ) : IBackupDataProvider {
        override val id: String = "messages"
        override val displayNameRes: Int = 0
        override val descriptionRes: Int = 0

        override suspend fun serialize(outputStream: java.io.OutputStream) {
            // Collect all messages from DB (simulated via loading all messaging keys)
            // In test we know the public keys we populated
            val sb = StringBuilder()
            for (i in 1..50) {
                val pk = "PublicKeyFriend_$i"
                val messages = messageRepository.get(pk).first()
                messages.forEach { m ->
                    sb.append(m.publicKey).append("|")
                        .append(m.message.replace("\n", "\\n").replace("|", "\\p")).append("|")
                        .append(m.sender.name).append("|")
                        .append(m.type.name).append("|")
                        .append(m.timestamp).append("\n")
                }
            }
            outputStream.write(sb.toString().encodeToByteArray())
        }

        override suspend fun deserialize(data: ByteArray) {
            val text = String(data)
            if (text.isBlank()) return
            text.split("\n").forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = line.split("|")
                if (parts.size >= 5) {
                    val messageText = parts[1].replace("\\n", "\n").replace("\\p", "|")
                    val message = Message(
                        publicKey = parts[0],
                        message = messageText,
                        sender = Sender.valueOf(parts[2]),
                        type = MessageType.valueOf(parts[3]),
                        timestamp = parts[4].toLong(),
                        correlationId = Int.MIN_VALUE
                    )
                    messageRepository.add(message)
                }
            }
        }
    }

    @Test
    fun testBackupStressExportImportCycle() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, Database::class.java).build()

        val contactRepository = ContactRepositoryImpl(db.contactDao())
        val messageRepository = MessageRepositoryImpl(db)

        // 1. Populate the DB with stress-test volume: 50 contacts, 500 messages
        val stressContacts = mutableListOf<Contact>()
        for (i in 1..50) {
            val contact = Contact(
                publicKey = "PublicKeyFriend_$i",
                name = "Friend Number $i",
                connectionStatus = if (i % 2 == 0) ConnectionStatus.UDP else ConnectionStatus.None
            )
            stressContacts.add(contact)
            contactRepository.add(contact)
        }

        val stressMessages = mutableListOf<Message>()
        for (i in 1..500) {
            val friendIndex = (i % 50) + 1
            val message = Message(
                publicKey = "PublicKeyFriend_$friendIndex",
                message = "This is stress-test message $i with some | separator and \n newline!",
                sender = if (i % 2 == 0) Sender.Sent else Sender.Received,
                type = if (i % 10 == 0) MessageType.Action else MessageType.Normal,
                timestamp = System.currentTimeMillis() - (500 - i) * 1000L,
                correlationId = Int.MIN_VALUE
            )
            stressMessages.add(message)
            messageRepository.add(message)
        }

        // Verify initial state
        val initialContactsCount = contactRepository.getAll().first().size
        assertEquals(50, initialContactsCount)

        // 2. Initialize backup components
        val platformServices = JvmPlatformServices()
        val contactsProvider = ContactsBackupProvider(contactRepository)
        val messagesProvider = MessagesBackupProvider(messageRepository)
        val providers = listOf(contactsProvider, messagesProvider)

        val exportBackup = ExportBackupUseCase(providers)
        val importBackup = ImportBackupUseCase(providers, platformServices)

        // 3. Export
        val backupBytes = exportBackup.execute(setOf("contacts", "messages"))

        assertTrue(backupBytes.isNotEmpty(), "Backup archive should not be empty")
        assertNotEquals(0, backupBytes.size)

        // 4. Verify that data is indeed encrypted and does not contain plain text identifiers
        val backupText = String(backupBytes, Charsets.ISO_8859_1)
        assertFalse(backupText.contains("PublicKeyFriend_"), "Backup should be fully encrypted")

        // 5. Hard purge the Room Database
        db.clearAllTables()

        // Verify that tables are empty
        assertEquals(0, contactRepository.getAll().first().size)
        for (i in 1..50) {
            assertEquals(0, messageRepository.get("PublicKeyFriend_$i").first().size)
        }

        // 6. Import back
        importBackup.execute(backupBytes)

        // 7. Verify integrity and equality of restored tables
        val restoredContacts = contactRepository.getAll().first().associateBy { it.publicKey }
        assertEquals(50, restoredContacts.size)

        stressContacts.forEach { original ->
            val restored = restoredContacts[original.publicKey]
            assertTrue(restored != null, "Contact should be restored")
            assertEquals(original.name, restored.name)
            assertEquals(original.connectionStatus, restored.connectionStatus)
        }

        // Verify messages restoration
        var totalRestoredMessages = 0
        for (i in 1..50) {
            val pk = "PublicKeyFriend_$i"
            val restoredList = messageRepository.get(pk).first()
            totalRestoredMessages += restoredList.size

            // Verify order and field integrity for each friend
            val originalFriendMessages = stressMessages.filter { it.publicKey == pk }
            assertEquals(originalFriendMessages.size, restoredList.size)
            originalFriendMessages.zip(restoredList).forEach { (orig, rest) ->
                assertEquals(orig.message, rest.message)
                assertEquals(orig.sender, rest.sender)
                assertEquals(orig.type, rest.type)
                assertEquals(orig.timestamp, rest.timestamp)
            }
        }
        assertEquals(500, totalRestoredMessages)

        db.close()
    }
}
