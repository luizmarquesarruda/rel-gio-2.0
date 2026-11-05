package com.luizmarquesarruda.relogio20.domain.telemetry

import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectTelemetryGate
import com.luizmarquesarruda.relogio20.data.healthconnect.ValidatedTelemetrySink
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test

class HealthConnectTelemetryGateTest {
    @Test
    fun candidateCannotBeWritten() = runBlocking {
        val gate = HealthConnectTelemetryGate(ValidatedTelemetrySink { })
        val sample = TelemetrySample(
            byteArrayOf(0x01), 100L, TrustLevel.CANDIDATO, "ab-parser-0",
            Provenance("GT3_MAX_NUS_TX")
        )
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { gate.writeIfEligible(sample) }
        }
    }

    @Test
    fun observedCannotBeWritten() = runBlocking {
        val gate = HealthConnectTelemetryGate(ValidatedTelemetrySink { })
        val sample = TelemetrySample(
            byteArrayOf(0x00), 100L, TrustLevel.OBSERVADO, "none",
            Provenance("GATT_READ")
        )
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { gate.writeIfEligible(sample) }
        }
    }
}
