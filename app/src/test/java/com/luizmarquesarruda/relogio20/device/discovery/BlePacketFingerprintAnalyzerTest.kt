package com.luizmarquesarruda.relogio20.device.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlePacketFingerprintAnalyzerTest {
    @Test
    fun repeatedPayloadGetsSameFingerprintAndIncreasingRepeatCount() {
        BlePacketFingerprintAnalyzer.resetSession()
        val payload = byteArrayOf(0xAB.toByte(), 0x00, 0x05, 0xFF.toByte(), 0x91.toByte())
        val first = BlePacketFingerprintAnalyzer.observe("service", "characteristic", payload)
        val second = BlePacketFingerprintAnalyzer.observe("service", "characteristic", payload)
        assertEquals(5, first.length)
        assertEquals(first.prefixHex, second.prefixHex)
        assertEquals(first.sha256Prefix, second.sha256Prefix)
        assertEquals(1, first.repeatedCount)
        assertEquals(2, second.repeatedCount)
        assertEquals(10, second.sha256Prefix.length)
    }

    @Test
    fun differentPayloadHasDifferentFingerprint() {
        BlePacketFingerprintAnalyzer.resetSession()
        val first = BlePacketFingerprintAnalyzer.observe("s", "c", byteArrayOf(1, 2, 3))
        val second = BlePacketFingerprintAnalyzer.observe("s", "c", byteArrayOf(1, 2, 4))
        assertNotEquals(first.sha256Prefix, second.sha256Prefix)
        assertTrue(second.repeatedCount == 1)
    }

    @Test
    fun resetStartsANewObservationSession() {
        val payload = byteArrayOf(9, 8, 7)
        BlePacketFingerprintAnalyzer.observe("s", "c", payload)
        BlePacketFingerprintAnalyzer.resetSession()
        val afterReset = BlePacketFingerprintAnalyzer.observe("s", "c", payload)
        assertEquals(1, afterReset.repeatedCount)
    }
}
