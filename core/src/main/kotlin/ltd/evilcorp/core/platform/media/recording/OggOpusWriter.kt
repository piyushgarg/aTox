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
class OggOpusWriter(private val outputStream: OutputStream) {
    private val serialNumber = Random.nextInt()
    private var pageSequenceNumber = 0
    private var granulePosition: Long = 0

    companion object {
        private const val CRC_TABLE_SIZE = 256
        private const val CRC_BIT_SHIFT = 24
        private const val CRC_ITERATIONS = 8
        private const val CRC_MSB_MASK = 31
        private const val CRC_POLYNOMIAL = 0x04C11DB7
        private const val BYTE_MASK = 0xFF

        private const val OPUS_HEAD_SIZE = 19
        private const val OPUS_VERSION = 1
        private const val OPUS_CHANNEL_COUNT = 1
        private const val OPUS_PRE_SKIP = 312
        private const val OPUS_SAMPLE_RATE = 48000
        private const val OPUS_GAIN = 0
        private const val OPUS_MAPPING_FAMILY = 0

        private const val MAX_SEGMENT_SIZE = 255
        private const val SEGMENT_REMAINDER = 254
        private const val OGG_HEADER_BASE_SIZE = 27
        private const val CRC_BYTE_OFFSET = 22

        private val crcTable = IntArray(CRC_TABLE_SIZE).apply {
            for (i in 0 until CRC_TABLE_SIZE) {
                var r = i shl CRC_BIT_SHIFT
                repeat(CRC_ITERATIONS) {
                    r = if ((r and (1 shl CRC_MSB_MASK)) != 0) {
                        (r shl 1) xor CRC_POLYNOMIAL
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
                val byteVal = b.toInt() and BYTE_MASK
                crc = (crc shl CRC_ITERATIONS) xor crcTable[((crc ushr CRC_BIT_SHIFT) xor byteVal) and BYTE_MASK]
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
        val buffer = ByteBuffer.allocate(OPUS_HEAD_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("OpusHead".toByteArray(Charsets.US_ASCII))
        buffer.put(OPUS_VERSION.toByte()) // version
        buffer.put(OPUS_CHANNEL_COUNT.toByte()) // channel count (mono)
        buffer.putShort(OPUS_PRE_SKIP.toShort()) // pre-skip
        buffer.putInt(OPUS_SAMPLE_RATE) // original sample rate (Opus decoders always output at 48000Hz internally)
        buffer.putShort(OPUS_GAIN.toShort()) // output gain
        buffer.put(OPUS_MAPPING_FAMILY.toByte()) // channel mapping family (0 = mono or stereo)
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
        val segmentCount = (payload.size + SEGMENT_REMAINDER) / MAX_SEGMENT_SIZE
        require(segmentCount <= MAX_SEGMENT_SIZE) { "Payload size is too large for single Ogg page" }

        val segmentTable = ByteArray(segmentCount)
        var remaining = payload.size
        for (i in 0 until segmentCount) {
            if (remaining >= MAX_SEGMENT_SIZE) {
                segmentTable[i] = MAX_SEGMENT_SIZE.toByte()
                remaining -= MAX_SEGMENT_SIZE
            } else {
                segmentTable[i] = remaining.toByte()
            }
        }

        val headerSize = OGG_HEADER_BASE_SIZE + segmentCount
        val pageSize = headerSize + payload.size
        val pageBuffer = ByteBuffer.allocate(pageSize).order(ByteOrder.LITTLE_ENDIAN)

        // Write page header fields
        pageBuffer.put("OggS".toByteArray(Charsets.US_ASCII)) // Capture Pattern
        pageBuffer.put(0.toByte()) // Stream structure version
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
        ByteBuffer.wrap(pageBytes).order(ByteOrder.LITTLE_ENDIAN).putInt(CRC_BYTE_OFFSET, crc)

        outputStream.write(pageBytes)
    }
}
