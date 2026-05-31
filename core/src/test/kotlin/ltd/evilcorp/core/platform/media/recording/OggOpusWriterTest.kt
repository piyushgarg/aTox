// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.platform.media.recording

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OggOpusWriterTest {

    @Test
    fun testCrcCalculation() {
        // Known test case for CRC: empty array CRC is 0
        val emptyCrc = OggOpusWriter.calculateCrc(ByteArray(0))
        assertEquals(0, emptyCrc)

        // Standard Ogg CRC-32 (polynomial 0x04C11DB7) calculation test
        val testData = "OggS".toByteArray(Charsets.US_ASCII)
        val calculatedCrc = OggOpusWriter.calculateCrc(testData)
        // Verify it doesn't crash and returns a non-zero value for non-empty input
        assertTrue(calculatedCrc != 0)
    }

    @Test
    fun testWriteHeaderAndPacket() {
        val outputStream = ByteArrayOutputStream()
        val writer = OggOpusWriter(outputStream)

        // 1. Write Header (OpusHead + OpusTags)
        writer.writeHeader()

        // 2. Write one packet
        val dummyPayload = byteArrayOf(0x1a, 0x2b, 0x3c, 0x4d, 0x5e)
        writer.writePacket(dummyPayload, sampleCount = 960)

        // 3. Close stream
        writer.close()

        val fileBytes = outputStream.toByteArray()
        assertTrue(fileBytes.isNotEmpty(), "Output bytes should not be empty")

        // Parse Ogg pages from output stream
        val buffer = ByteBuffer.wrap(fileBytes).order(ByteOrder.LITTLE_ENDIAN)

        // --- Page 1: OpusHead ---
        // Verify capture pattern "OggS"
        val capturePattern1 = ByteArray(4)
        buffer.get(capturePattern1)
        assertEquals("OggS", String(capturePattern1, Charsets.US_ASCII))
        
        // Skip some header fields: version (1), header type (1), granule (8), serial (4), seq (4), crc (4)
        buffer.position(buffer.position() + 22)
        val segmentCount1 = buffer.get().toInt() and 0xFF
        assertEquals(1, segmentCount1)
        
        val segmentSize1 = buffer.get().toInt() and 0xFF
        assertEquals(19, segmentSize1) // OpusHead payload size is 19 bytes
        
        val opusHeadMagic = ByteArray(8)
        buffer.get(opusHeadMagic)
        assertEquals("OpusHead", String(opusHeadMagic, Charsets.US_ASCII))
        
        // Skip remaining 11 bytes of OpusHead payload
        buffer.position(buffer.position() + 11)

        // --- Page 2: OpusTags ---
        val capturePattern2 = ByteArray(4)
        buffer.get(capturePattern2)
        assertEquals("OggS", String(capturePattern2, Charsets.US_ASCII))
        
        // Skip header fields
        buffer.position(buffer.position() + 22)
        val segmentCount2 = buffer.get().toInt() and 0xFF
        assertEquals(1, segmentCount2)
        
        val segmentSize2 = buffer.get().toInt() and 0xFF
        
        val opusTagsMagic = ByteArray(8)
        buffer.get(opusTagsMagic)
        assertEquals("OpusTags", String(opusTagsMagic, Charsets.US_ASCII))
        
        // Skip remaining payload bytes of OpusTags
        buffer.position(buffer.position() + (segmentSize2 - 8))

        // --- Page 3: Audio Packet ---
        val capturePattern3 = ByteArray(4)
        buffer.get(capturePattern3)
        assertEquals("OggS", String(capturePattern3, Charsets.US_ASCII))
        
        // Check Header Type (flags) is 0
        buffer.get() // version
        val headerTypeFlags = buffer.get().toInt() and 0xFF
        assertEquals(0, headerTypeFlags) // 0x00 for normal packet
        
        // Check Granule Position is 960
        val granulePos = buffer.getLong()
        assertEquals(960L, granulePos)
        
        // Skip serial, page sequence
        buffer.position(buffer.position() + 8)
        
        // Skip CRC field (4 bytes)
        buffer.position(buffer.position() + 4)
        
        // Page segments
        val segmentCount3 = buffer.get().toInt() and 0xFF
        assertEquals(1, segmentCount3)
        
        val segmentSize3 = buffer.get().toInt() and 0xFF
        assertEquals(dummyPayload.size, segmentSize3)
        
        val writtenPayload = ByteArray(segmentSize3)
        buffer.get(writtenPayload)
        
        // Verify payload matches what we wrote
        for (i in dummyPayload.indices) {
            assertEquals(dummyPayload[i], writtenPayload[i])
        }
    }
}
