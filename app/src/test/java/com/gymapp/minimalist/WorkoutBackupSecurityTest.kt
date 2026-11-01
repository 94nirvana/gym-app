package com.gymapp.minimalist

import com.gymapp.minimalist.data.security.CryptoUtils
import com.gymapp.minimalist.domain.BackupPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutBackupSecurityTest {

    @Test
    fun `sha256 calculation produces consistent 64 character hex hash`() {
        val input = "GymTrack-Secure-Payload-12345"
        val hash = CryptoUtils.computeSha256(input)
        assertEquals(64, hash.length)
        assertEquals(hash, CryptoUtils.computeSha256(input))
    }

    @Test
    fun `sha256 detects payload tampering immediately`() {
        val original = "{\"plans\":[{\"letter\":\"A\",\"customName\":\"Peito\"}]}"
        val tampered = "{\"plans\":[{\"letter\":\"A\",\"customName\":\"P3ito\"}]}"

        val originalHash = CryptoUtils.computeSha256(original)
        val tamperedHash = CryptoUtils.computeSha256(tampered)

        assertNotEquals(originalHash, tamperedHash)
    }

    @Test
    fun `string sanitization strips dangerous control characters and limits length`() {
        val maliciousInput = "Supino Reto\u0000\u0007\u001B[31m Teste"
        val sanitized = CryptoUtils.sanitizeString(maliciousInput, 20)

        assertTrue(!sanitized.contains("\u0000"))
        assertTrue(!sanitized.contains("\u0007"))
        assertTrue(!sanitized.contains("\u001B"))
        assertTrue(sanitized.length <= 20)
    }

    @Test
    fun `max backup payload size limit is 2MB to protect against DoS`() {
        assertEquals(2 * 1024 * 1024, BackupPayload.MAX_JSON_SIZE_BYTES)
    }
}
