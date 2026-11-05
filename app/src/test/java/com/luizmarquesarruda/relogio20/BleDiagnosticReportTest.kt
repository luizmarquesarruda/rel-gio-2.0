package com.luizmarquesarruda.relogio20

import com.luizmarquesarruda.relogio20.domain.diagnostics.BleDiagnosticReport

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BleDiagnosticReportTest {
    @Test
    fun masksBluetoothAddressInHeaderAndLog() {
        val report = BleDiagnosticReport.build(
            appVersion = "0.4.0",
            androidVersion = "16 (API 36)",
            connectionState = "Bluetooth conectado",
            deviceName = "GT3 MAX",
            deviceAddress = "C0:27:AB:64:6C:E2",
            generatedAt = "2026-11-04 12:00:00 +0000",
            logLines = listOf("Conectado ao C0:27:AB:64:6C:E2")
        )
        assertTrue(report.contains("XX:XX:XX:64:6C:E2"))
        assertFalse(report.contains("C0:27:AB:64:6C:E2"))
    }

    @Test
    fun marksProtocolInterpretationsAsUnvalidated() {
        val report = BleDiagnosticReport.build(
            appVersion = "0.4.0",
            androidVersion = "16 (API 36)",
            connectionState = "Desconectado",
            deviceName = null,
            deviceAddress = null,
            generatedAt = "2026-11-04 12:00:00 +0000",
            logLines = emptyList()
        )
        assertTrue(report.contains("não validada"))
        assertTrue(report.contains("Nenhum evento registrado"))
        assertTrue(report.contains("não disponível"))
    }

    @Test
    fun includesPersistedDiscoveriesAndMasksAddresses() {
        val report = BleDiagnosticReport.build(
            appVersion = "0.4.0",
            androidVersion = "16 (API 36)",
            connectionState = "Procurando relógio",
            deviceName = null,
            deviceAddress = null,
            generatedAt = "2026-11-04 16:00:00 -0300",
            logLines = emptyList(),
            discoveryEntries = listOf("GT3 MAX | C0:27:AB:64:6C:E2 | fingerprint observado")
        )
        assertTrue(report.contains("[DESCOBERTAS PERSISTIDAS"))
        assertTrue(report.contains("XX:XX:XX:64:6C:E2"))
        assertFalse(report.contains("C0:27:AB:64:6C:E2"))
    }

    @Test
    fun preservesObservedHexPayloadForOfflineAnalysis() {
        val report = BleDiagnosticReport.build(
            appVersion = "0.4.0",
            androidVersion = "16 (API 36)",
            connectionState = "Bluetooth conectado",
            deviceName = "GT3 MAX",
            deviceAddress = null,
            generatedAt = "2026-11-04 12:00:00 +0000",
            logLines = listOf("NOTIFICAÇÃO 0000-0000 | 8 bytes: AB-00-05-FF-91-80-00-0C")
        )
        assertTrue(report.contains("AB-00-05-FF-91-80-00-0C"))
    }
}
