package com.luizmarquesarruda.relogio20

import com.luizmarquesarruda.relogio20.domain.diagnostics.BleDiagnosticReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BleDiagnosticReportTest {
    @Test
    fun reportOmitsDeviceIdentityAndRawLogContents() {
        val report =
            BleDiagnosticReport.build(
                appVersion = "0.4.0",
                androidVersion = "16 (API 36)",
                connectionState = "Bluetooth conectado",
                deviceName = "Luiz GT3 MAX C0:27:AB:64:6C:E2",
                deviceAddress = "C0:27:AB:64:6C:E2",
                generatedAt = "2026-11-04 12:00:00 +0000",
                logLines =
                    listOf(
                        "Frequência cardíaca: 147 bpm",
                        "NOTIFICAÇÃO: AB-00-05-FF-91-80-00-0C",
                    ),
                discoveryEntries =
                    listOf(
                        "Nome=Luiz GT3 MAX | HEX=AB-00-05-FF-91-80-00-0C | C0:27:AB:64:6C:E2",
                    ),
            )

        assertTrue(report.contains("identificação ocultada"))
        assertTrue(report.contains("Eventos técnicos omitidos por privacidade: 2"))
        assertTrue(report.contains("Registros técnicos omitidos por privacidade: 1"))
        assertFalse(report.contains("Luiz"))
        assertFalse(report.contains("C0:27:AB:64:6C:E2"))
        assertFalse(report.contains("XX:XX:XX"))
        assertFalse(report.contains("147 bpm"))
        assertFalse(report.contains("AB-00-05-FF-91-80-00-0C"))
        assertFalse(report.contains("HEX="))
    }

    @Test
    fun reportKeepsValidationWarningsWithoutInventingSensorData() {
        val report =
            BleDiagnosticReport.build(
                appVersion = "0.4.0",
                androidVersion = "16 (API 36)",
                connectionState = "Desconectado",
                deviceName = null,
                deviceAddress = null,
                generatedAt = "2026-11-04 12:00:00 +0000",
                logLines = emptyList(),
            )

        assertTrue(report.contains("não validada"))
        assertTrue(report.contains("Eventos técnicos omitidos por privacidade: 0"))
        assertTrue(report.contains("Registros técnicos omitidos por privacidade: 0"))
        assertFalse(report.contains("não disponível"))
    }

    @Test
    fun addressMaskNeverRetainsAnyAddressOctets() {
        assertEquals(
            "identificação ocultada",
            BleDiagnosticReport.maskAddress("C0:27:AB:64:6C:E2"),
        )
        assertEquals(
            "identificação ocultada",
            BleDiagnosticReport.maskAddress("invalid-device-id"),
        )
    }
}
