package ltd.evilcorp.core.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
class AvatarRepositoryImplTest {

    private lateinit var context: android.content.Context
    private lateinit var repository: AvatarRepositoryImpl
    private lateinit var filesDir: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        filesDir = context.filesDir
        repository = AvatarRepositoryImpl(context)

        // Clear files before test
        File(filesDir, "self_avatar.png").delete()
        File(filesDir, "self_avatar.jpg").delete()
    }

    @After
    fun tearDown() {
        File(filesDir, "self_avatar.png").delete()
        File(filesDir, "self_avatar.jpg").delete()
    }

    @Test
    fun testSaveSelfAvatar_savesBytesToJpg() = runTest {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val success = repository.saveSelfAvatar(bytes)

        assertTrue(success)
        val file = repository.getSelfAvatarFile()
        assertEquals("self_avatar.jpg", file.name)
        assertTrue(file.exists())
        assertEquals(5L, file.length())
        assertEquals(2, file.readBytes()[1])
    }

    @Test
    fun testGetSelfAvatarFile_migratesPngToJpg() {
        val oldFile = File(filesDir, "self_avatar.png")
        val newFile = File(filesDir, "self_avatar.jpg")

        assertFalse(oldFile.exists())
        assertFalse(newFile.exists())

        // Create old file
        oldFile.writeBytes(byteArrayOf(9, 8, 7))
        assertTrue(oldFile.exists())

        // Retrieve self avatar file, which should trigger migration
        val retrievedFile = repository.getSelfAvatarFile()

        assertEquals(newFile.absolutePath, retrievedFile.absolutePath)
        assertTrue(newFile.exists())
        assertFalse(oldFile.exists())
        assertEquals(3L, newFile.length())
    }
}
