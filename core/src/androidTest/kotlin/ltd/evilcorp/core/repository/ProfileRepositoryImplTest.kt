package ltd.evilcorp.core.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import ltd.evilcorp.core.db.Database
import ltd.evilcorp.core.db.entity.ContactEntity
import ltd.evilcorp.domain.core.model.PublicKey
import ltd.evilcorp.domain.core.network.IToxStarter
import ltd.evilcorp.domain.core.network.save.ISaveManager
import ltd.evilcorp.domain.core.network.save.ToxSaveStatus
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ProfileRepositoryImplTest {

    private lateinit var context: android.content.Context
    private lateinit var db: Database
    private lateinit var toxStarter: FakeToxStarter
    private lateinit var saveManager: FakeSaveManager
    private lateinit var repository: ProfileRepositoryImpl

    private class FakeToxStarter : IToxStarter {
        var stopCalled = false
        override fun tryLoadTox(password: String?): ToxSaveStatus = ToxSaveStatus.Ok
        override fun stopTox() {
            stopCalled = true
        }
        override fun startTox(save: ByteArray?, password: String?): ToxSaveStatus = ToxSaveStatus.Ok
    }

    private class FakeSaveManager : ISaveManager {
        val profiles = mutableMapOf<String, ByteArray>()
        override fun list(): List<String> = profiles.keys.toList()
        override fun save(pk: PublicKey, saveData: ByteArray) {
            profiles[pk.string()] = saveData
        }
        override fun load(pk: PublicKey): ByteArray? = profiles[pk.string()]
        override fun delete(pk: PublicKey): Boolean {
            return profiles.remove(pk.string()) != null
        }
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // We use a real database file for testing checkpoint creation and restoration,
        // because inMemoryDatabaseBuilder does not have real backing files on disk to copy!
        // To avoid conflicts, we use a unique test database name.
        context.deleteDatabase("core_db") // clear any residual
        db = Room.databaseBuilder(context, Database::class.java, "core_db")
            .allowMainThreadQueries()
            .build()
            
        toxStarter = FakeToxStarter()
        saveManager = FakeSaveManager()
        
        repository = ProfileRepositoryImpl(context, toxStarter, saveManager, db)
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase("core_db")
        
        val checkpointDir = File(context.cacheDir, "database_checkpoint")
        if (checkpointDir.exists()) {
            checkpointDir.deleteRecursively()
        }
    }

    @Test
    fun testDeleteProfile_stopsToxAndClearsSavesAndDatabase() = runTest {
        // Seed database
        val contact = ContactEntity("123", "Alice")
        db.contactDao().save(contact)
        assertEquals(1, db.contactDao().loadAll().first().size)

        // Seed saveManager
        val pk = PublicKey("ABCDEF1234567890")
        saveManager.save(pk, byteArrayOf(1, 2))
        saveManager.save(PublicKey("FEDCBA0987654321"), byteArrayOf(3, 4))
        assertEquals(2, saveManager.list().size)

        repository.deleteProfile(pk)

        // Tox stopped
        assertTrue(toxStarter.stopCalled)

        // All saves deleted
        assertTrue(saveManager.list().isEmpty())

        // Database tables cleared
        assertTrue(db.contactDao().loadAll().first().isEmpty())
    }

    @Test
    fun testClearDatabase_clearsAllTables() = runTest {
        val contact = ContactEntity("123", "Alice")
        db.contactDao().save(contact)
        assertEquals(1, db.contactDao().loadAll().first().size)

        repository.clearDatabase()
        assertTrue(db.contactDao().loadAll().first().isEmpty())
    }

    @Test
    fun testCheckpointLifecycle() = runTest {
        // 1. Seed database with unique data
        val contact = ContactEntity("12345", "Checkpoint Contact")
        db.contactDao().save(contact)
        assertEquals(1, db.contactDao().loadAll().first().size)

        // 2. Create checkpoint
        val created = repository.createCheckpoint()
        assertTrue(created)

        // Checkpoint directory should exist and have files
        val checkpointDir = File(context.cacheDir, "database_checkpoint")
        assertTrue(checkpointDir.exists())
        assertTrue(checkpointDir.listFiles()?.isNotEmpty() ?: false)

        // 3. Re-open db, modify it to check restore
        db = Room.databaseBuilder(context, Database::class.java, "core_db")
            .allowMainThreadQueries()
            .build()
        // Repository needs to point to the new DB instance
        repository = ProfileRepositoryImpl(context, toxStarter, saveManager, db)
        
        db.contactDao().save(ContactEntity("99999", "New Contact"))
        assertEquals(2, db.contactDao().loadAll().first().size)

        // 4. Restore from checkpoint
        val restored = repository.restoreFromCheckpoint()
        assertTrue(restored)

        // 5. Re-open restored db and verify content
        db = Room.databaseBuilder(context, Database::class.java, "core_db")
            .allowMainThreadQueries()
            .build()
        
        val restoredContacts = db.contactDao().loadAll().first()
        assertEquals(1, restoredContacts.size)
        assertEquals("12345", restoredContacts[0].publicKey)
        assertEquals("Checkpoint Contact", restoredContacts[0].name)

        // 6. Clear checkpoint
        repository = ProfileRepositoryImpl(context, toxStarter, saveManager, db)
        repository.clearCheckpoint()
        assertFalse(checkpointDir.exists())
    }
}
