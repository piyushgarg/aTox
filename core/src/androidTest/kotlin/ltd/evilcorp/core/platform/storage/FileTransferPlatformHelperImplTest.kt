// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.platform.storage

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class FileTransferPlatformHelperImplTest {

    @Test
    fun testDirectories() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FileTransferPlatformHelperImpl(context)

        val filesDir = helper.getFilesDir()
        assertEquals(context.filesDir.absolutePath, filesDir)

        val cacheDir = helper.getCacheDir()
        assertEquals(context.cacheDir.absolutePath, cacheDir)
    }

    @Test
    fun testFileOperationsWithFileUri() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FileTransferPlatformHelperImpl(context)

        // Create a dummy test file
        val tempFile = File(context.cacheDir, "test_file_transfer.txt")
        val contentString = "aTox File Transfer Platform Helper Test Content"
        tempFile.writeText(contentString)
        val fileUriString = Uri.fromFile(tempFile).toString()

        try {
            // Test getFileSizeAndName
            val nameAndSize = helper.getFileSizeAndName(fileUriString)
            assertNotNull(nameAndSize)
            assertEquals("test_file_transfer.txt", nameAndSize.first)
            assertEquals(tempFile.length(), nameAndSize.second)

            // Test openInputStream and read content
            val inputStream = helper.openInputStream(fileUriString)
            assertNotNull(inputStream)
            
            val buffer = ByteArray(contentString.length)
            val bytesRead = inputStream.read(buffer, 0, buffer.size)
            assertEquals(buffer.size, bytesRead)
            assertEquals(contentString, String(buffer, Charsets.UTF_8))
            inputStream.close()

            // Test copyToOutgoingCache
            val cacheUriString = helper.copyToOutgoingCache(fileUriString, "copied_test.txt")
            assertNotNull(cacheUriString)
            assertTrue(cacheUriString.startsWith("file://"))

            val cacheFile = File(Uri.parse(cacheUriString).path!!)
            assertTrue(cacheFile.exists())
            assertEquals(contentString, cacheFile.readText())

            // Cleanup copied cache file
            cacheFile.delete()
        } finally {
            // Cleanup temp file
            tempFile.delete()
        }
    }
}
