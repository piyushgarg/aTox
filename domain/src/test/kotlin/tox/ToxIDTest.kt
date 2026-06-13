package ltd.evilcorp.domain.core.network

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ToxIDTest {
    @Test
    fun testToxIDValidation() {
        val pubKey = "76518406F6A9F2217E8DC487CC783C25CC16A15EB36FF32E335A235342C48A39"
        val nospam = "00000000"
        val payload = pubKey + nospam
        val checksumInt = payload.chunked(4).map { it.toInt(16) }.fold(0) { b1, b2 -> b1 xor b2 }
        val checksum = checksumInt.toString(16).uppercase().padStart(4, '0')
        val validToxId = pubKey + nospam + checksum
        
        assertTrue(ToxID.isValid(validToxId))
        assertTrue(ToxID.isValid(validToxId.lowercase()))
        
        // Invalid length
        assertFalse(ToxID.isValid(validToxId.drop(1)))
        // Invalid checksum
        assertFalse(ToxID.isValid(pubKey + nospam + "0000"))
        // Invalid chars - replacing a known valid hex char with 'z'
        val invalidCharToxId = validToxId.replace(validToxId[0], 'z')
        assertFalse(ToxID.isValid(invalidCharToxId))
    }

    @Test
    fun testToxIDConstruction_failsOnInvalid() {
        val pubKey = "76518406F6A9F2217E8DC487CC783C25CC16A15EB36FF32E335A235342C48A39"
        val nospam = "00000000"
        val payload = pubKey + nospam
        val checksumInt = payload.chunked(4).map { it.toInt(16) }.fold(0) { b1, b2 -> b1 xor b2 }
        val checksum = checksumInt.toString(16).uppercase().padStart(4, '0')
        val validToxId = pubKey + nospam + checksum
        
        assertTrue(ToxID.isValid(validToxId))
        assertFalse(ToxID.isValid("invalid"))
        assertFalse(ToxID.isValid(pubKey + nospam + "0000"))
    }
}
