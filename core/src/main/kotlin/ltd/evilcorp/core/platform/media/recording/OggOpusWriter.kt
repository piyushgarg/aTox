// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.core.platform.media.recording

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random

/**
 * Ogg page encapsulator for Opus audio packets.
 * Produces standard-compliant .opus files that can be natively played back by Android MediaPlayer.
 */
@Suppress("MagicNumber")
class OggOpusWriter(private val outputStream: OutputStream) {
    private val serialNumber = Random.nextInt()
    private var pageSequenceNumber = 0
    private var granulePosition: Long = 0

    companion object {
        private val crcTable = IntArray(256).apply {
            for (i in 0 until 256) {
                var r = i shl 24
                repeat(8) {
                    r = if ((r and (1 shl 31)) != 0) {
                        (r shl 1) xor 0x04C11DB7
                    } else {
                        r shl 1
                    }
                }
                this[i] = r
            }
        }

        /**
         * Calculates Ogg CRC32 checksum over the entire page.
         */
        fun calculateCrc(data: ByteArray): Int {
            var crc = 0
            for (b in data) {
                val byteVal = b.toInt() and 0xFF
                crc = (crc shl 8) xor crcTable[((crc ushr 24) xor byteVal) and 0xFF]
            }
            return crc
        }
    }

    /**
     * Writes the initial Ogg Opus headers (OpusHead and OpusTags pages).
     */
    fun writeHeader() {
        // 1. Write OpusHead Page (BOS - Beginning of Stream)
        val opusHeadPayload = createOpusHead()
        writePage(opusHeadPayload, flags = 0x02, granulePos = 0)

        // 2. Write OpusTags Page
        val opusTagsPayload = createOpusTags()
        writePage(opusTagsPayload, flags = 0x00, granulePos = 0)
    }

    /**
     * Encapsulates and writes an encoded Opus audio frame to the file.
     * @param encodedData raw encoded Opus bytes
     * @param sampleCount number of samples represented by this packet (e.g. 960 for 20ms at 48kHz)
     */
    fun writePacket(encodedData: ByteArray, sampleCount: Int) {
        granulePosition += sampleCount
        writePage(encodedData, flags = 0x00, granulePos = granulePosition)
    }

    /**
     * Flushes and closes the stream.
     */
    fun close() {
        outputStream.flush()
        outputStream.close()
    }

    private fun createOpusHead(): ByteArray {
        val buffer = ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("OpusHead".toByteArray(Charsets.US_ASCII))
        buffer.put(1) // version
        buffer.put(1) // channel count (mono)
        buffer.putShort(312) // pre-skip
        buffer.putInt(48000) // original sample rate (Opus decoders always output at 48000Hz internally)
        buffer.putShort(0) // output gain
        buffer.put(0) // channel mapping family (0 = mono or stereo)
        return buffer.array()
    }

    private fun createOpusTags(): ByteArray {
        val vendor = "libopus (aTox)".toByteArray(Charsets.UTF_8)
        val size = 8 + 4 + vendor.size + 4 // Magic + vendor len + vendor + comments count (0)
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("OpusTags".toByteArray(Charsets.US_ASCII))
        buffer.putInt(vendor.size)
        buffer.put(vendor)
        buffer.putInt(0) // comment list length (0 comments)
        return buffer.array()
    }

    private fun writePage(payload: ByteArray, flags: Int, granulePos: Long) {
        val segmentCount = (payload.size + 254) / 255
        require(segmentCount <= 255) { "Payload size is too large for single Ogg page" }

        val segmentTable = ByteArray(segmentCount)
        var remaining = payload.size
        for (i in 0 until segmentCount) {
            if (remaining >= 255) {
                segmentTable[i] = 255.toByte()
                remaining -= 255
            } else {
                segmentTable[i] = remaining.toByte()
            }
        }

        val headerSize = 27 + segmentCount
        val pageSize = headerSize + payload.size
        val pageBuffer = ByteBuffer.allocate(pageSize).order(ByteOrder.LITTLE_ENDIAN)

        // Write page header fields
        pageBuffer.put("OggS".toByteArray(Charsets.US_ASCII)) // Capture Pattern
        pageBuffer.put(0) // Stream structure version
        pageBuffer.put(flags.toByte()) // Header type flags
        pageBuffer.putLong(granulePos) // Granule position
        pageBuffer.putInt(serialNumber) // Stream serial number
        pageBuffer.putInt(pageSequenceNumber++) // Page sequence number
        pageBuffer.putInt(0) // Checksum (temporarily 0)
        pageBuffer.put(segmentCount.toByte()) // Page segments
        pageBuffer.put(segmentTable) // Segment table
        pageBuffer.put(payload) // Payload data

        val pageBytes = pageBuffer.array()

        // Calculate CRC checksum over the entire page (with checksum field set to 0)
        val crc = calculateCrc(pageBytes)

        // Write the calculated CRC into bytes 22..25 of the page
        ByteBuffer.wrap(pageBytes).order(ByteOrder.LITTLE_ENDIAN).putInt(22, crc)

        outputStream.write(pageBytes)
    }
}
