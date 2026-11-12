package com.luizmarquesarruda.relogio20.domain.telemetry

import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectTelemetryGate
import com.luizmarquesarruda.relogio20.data.healthconnect.ValidatedTelemetrySink
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertThrows
import org.junit.Test

class HealthConnectTelemetryGateTest {
    @Test
    fun candidateCannotBeWritten() {
        var writes = 0
        val gate = HealthConnectTelemetryGate(ValidatedTelemetrySink { writes += 1 })
        val sample =
            TelemetrySample(
                byteArrayOf(0x01),
                100L,
                TrustLevel.CANDIDATO,
                "ab-parser-0",
                Provenance("GT3_MAX_NUS_TX"),
            )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { gate.writeIfEligible(sample) }
        }
        assertEquals(0, writes)
    }

    @Test
    fun observedCannotBeWritten() {
        var writes = 0
        val gate = HealthConnectTelemetryGate(ValidatedTelemetrySink { writes += 1 })
        val sample =
            TelemetrySample(
                byteArrayOf(0x00),
                100L,
                TrustLevel.OBSERVADO,
                "none",
                Provenance("GATT_READ"),
            )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { gate.writeIfEligible(sample) }
        }
        assertEquals(0, writes)
    }

    @Test
    fun validatedSampleIsWrittenAsDefensiveCopy() {
        val raw = byteArrayOf(0x02, 0x03)
        val sample =
            TelemetrySample(
                raw,
                200L,
                TrustLevel.VALIDADO,
                "sig-parser-1",
                Provenance("GATT_READ"),
            )
        var written: TelemetrySample? = null
        val gate = HealthConnectTelemetryGate(ValidatedTelemetrySink { written = it })

        runBlocking { gate.writeIfEligible(sample) }

        val received = requireNotNull(written)
        assertArrayEquals(raw, received.raw)
        assertNotSame(raw, received.raw)
        assertNotSame(sample, received)
    }
}
