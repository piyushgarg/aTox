// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.platform.storage

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileStorageProviderTest {

    private lateinit var provider: AndroidFileStorageProvider
    private lateinit var tempFile: File

    @BeforeTest
    fun setUp() {
        provider = AndroidFileStorageProvider()
        tempFile = File.createTempFile("test_storage_provider", ".txt")
        tempFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
    }

    @AfterTest
    fun tearDown() {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    @Test
    fun testExists_realFile_returnsTrue() {
        val uriString = "file://${tempFile.absolutePath}"
        assertTrue(provider.exists(uriString))
    }

    @Test
    fun testExists_nonExistentFile_returnsFalse() {
        val uriString = "file://${tempFile.absolutePath}_does_not_exist"
        assertFalse(provider.exists(uriString))
    }

    @Test
    fun testSize_realFile_returnsCorrectSize() {
        val uriString = "file://${tempFile.absolutePath}"
        assertEquals(5L, provider.size(uriString))
    }

    @Test
    fun testSize_nonExistentFile_returnsZero() {
        val uriString = "file://${tempFile.absolutePath}_does_not_exist"
        assertEquals(0L, provider.size(uriString))
    }

    @Test
    fun testLastModified_realFile_returnsNonZero() {
        val uriString = "file://${tempFile.absolutePath}"
        assertTrue(provider.lastModified(uriString) > 0L)
    }

    @Test
    fun testLastModified_nonExistentFile_returnsZero() {
        val uriString = "file://${tempFile.absolutePath}_does_not_exist"
        assertEquals(0L, provider.lastModified(uriString))
    }

    @Test
    fun testGetAbsolutePath_realFile_returnsCorrectPath() {
        val uriString = "file://${tempFile.absolutePath}"
        assertEquals(tempFile.absolutePath, provider.getAbsolutePath(uriString))
    }

    @Test
    fun testGetAbsolutePath_emptyUri_returnsNull() {
        assertNull(provider.getAbsolutePath(""))
    }
}
