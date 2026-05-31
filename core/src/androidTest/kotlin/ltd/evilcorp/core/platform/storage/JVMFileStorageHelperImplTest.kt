package ltd.evilcorp.core.platform.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ltd.evilcorp.domain.core.io.IInputStream
import ltd.evilcorp.domain.features.transfer.IFileTransferPlatformHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class JVMFileStorageHelperImplTest {

    private lateinit var tempDir: File
    private lateinit var platformHelper: IFileTransferPlatformHelper
    private lateinit var storageHelper: JVMFileStorageHelperImpl

    private class FakeFileTransferPlatformHelper(private val dir: File) : IFileTransferPlatformHelper {
        override fun getFilesDir(): String = dir.absolutePath
        override fun getCacheDir(): String = dir.absolutePath
        override fun getFileSizeAndName(uriString: String): Pair<String, Long>? = null
        override fun copyToOutgoingCache(uriString: String, name: String): String = ""
        override fun openInputStream(uriString: String): IInputStream? = null
        override fun releaseFilePermission(uriString: String) {}
        override fun autoSaveFileToPublicDownloads(fileName: String, sourceFilePath: String): String? = null
        override fun autoSaveFileToDirectory(fileName: String, sourceFilePath: String, directoryUriString: String): String? = null
        override fun saveFileToUri(sourceFilePath: String, targetUriString: String): Boolean = false
    }

    @Before
    fun setUp() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        tempDir = File(appContext.cacheDir, "test_file_storage_helper_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        
        platformHelper = FakeFileTransferPlatformHelper(tempDir)
        storageHelper = JVMFileStorageHelperImpl(platformHelper)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testInit_createsSubdirectories() {
        val ftDir = File(tempDir, "ft")
        val avatarDir = File(tempDir, "avatar")
        assertTrue(ftDir.exists() && ftDir.isDirectory)
        assertTrue(avatarDir.exists() && avatarDir.isDirectory)
    }

    @Test
    fun testCreateEmptyFile_and_writeChunk_and_delete() {
        val filePath = File(tempDir, "test_file.dat").absolutePath
        val uri = "file://$filePath"
        
        assertTrue(storageHelper.createEmptyFile(uri, 10L))
        assertTrue(storageHelper.fileExists(uri))
        assertEquals(10L, File(filePath).length())

        val data = byteArrayOf(65, 66, 67) // 'A', 'B', 'C'
        assertTrue(storageHelper.writeChunk(uri, 2L, data))
        
        val content = File(filePath).readBytes()
        assertEquals(65, content[2])
        assertEquals(66, content[3])
        assertEquals(67, content[4])

        assertTrue(storageHelper.deleteFile(uri))
        assertFalse(storageHelper.fileExists(uri))
    }

    @Test
    fun testMakeLocalDestinationPath() {
        val path = storageHelper.makeLocalDestinationPath("fingerprint", "photo.png")
        assertTrue(path.endsWith(".png"))
        assertTrue(path.contains("ft/fingerprint/"))
        
        val parentFile = File(path).parentFile
        assertNotNull(parentFile)
        assertTrue(parentFile.exists())
    }

    @Test
    fun testMakeWipAvatarPath_and_finalizeAvatar() {
        val wipUri = storageHelper.makeWipAvatarPath("avatar123")
        assertTrue(wipUri.startsWith("file://"))
        assertTrue(wipUri.contains("avatar/avatar123.wip"))

        val wipPath = storageHelper.getPathFromUri(wipUri)
        assertNotNull(wipPath)
        val wipFile = File(wipPath)
        wipFile.writeBytes(byteArrayOf(1, 2, 3))

        val finalUri = storageHelper.finalizeAvatar("avatar123", "final_avatar.png")
        assertNotNull(finalUri)
        assertTrue(finalUri.startsWith("file://"))
        assertTrue(finalUri.contains("avatar/final_avatar.png"))
        
        val finalPath = storageHelper.getPathFromUri(finalUri)
        assertNotNull(finalPath)
        val finalFile = File(finalPath)
        assertTrue(finalFile.exists())
        assertEquals(3L, finalFile.length())
        assertFalse(wipFile.exists())
    }

    @Test
    fun testGetSelfAvatarInfo() {
        assertNull(storageHelper.getSelfAvatarInfo())

        val selfAvatar = File(tempDir, "self_avatar.png")
        selfAvatar.writeBytes(byteArrayOf(9, 9, 9))

        val avatarInfo = storageHelper.getSelfAvatarInfo()
        assertNotNull(avatarInfo)
        assertTrue(avatarInfo.first.startsWith("file://"))
        assertEquals(3L, avatarInfo.second)
    }

    @Test
    fun testGetCacheSize_and_clearCache() {
        val ftDir = File(tempDir, "ft")
        val testFile = File(ftDir, "dummy.bin")
        testFile.writeBytes(byteArrayOf(1, 1, 1, 1, 1))

        assertEquals(5L, storageHelper.getCacheSize())
        assertTrue(storageHelper.clearCache())
        assertEquals(0L, storageHelper.getCacheSize())
    }

    @Test
    fun testGetPathFromUri() {
        assertEquals("/path/to/file", storageHelper.getPathFromUri("file:///path/to/file"))
        assertNull(storageHelper.getPathFromUri("http://google.com"))
        assertNull(storageHelper.getPathFromUri(""))
    }
}
