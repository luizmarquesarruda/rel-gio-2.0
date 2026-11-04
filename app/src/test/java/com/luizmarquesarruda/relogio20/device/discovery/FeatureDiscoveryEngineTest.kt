package com.luizmarquesarruda.relogio20.device.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureDiscoveryEngineTest {
    @Test
    fun findsStandardHeartRateProfileWithoutClaimingMeasurementIsValidated() {
        val snapshot = FeatureDiscoveryEngine.captureInventory(
            listOf(
                GattServiceRecord(
                    uuid = "0000180d-0000-1000-8000-00805f9b34fb",
                    characteristics = listOf(
                        GattCharacteristicRecord(
                            uuid = "00002a37-0000-1000-8000-00805f9b34fb",
                            properties = setOf("NOTIFY")
                        )
                    )
                )
            ),
            capturedAtMillis = 100L
        )
        val finding = snapshot.findings.firstOrNull { it.id == "standard:180d" }
        assertNotNull(finding)
        assertTrue(finding?.status?.contains("NÃO VALIDADOS") == true)
        assertTrue(finding?.detail?.contains("Nenhuma leitura") == true)
    }

    @Test
    fun detectsStandardCurrentTimeCharacteristicAndItsWriteCapability() {
        val snapshot = FeatureDiscoveryEngine.captureInventory(
            listOf(
                GattServiceRecord(
                    uuid = "00001805-0000-1000-8000-00805f9b34fb",
                    characteristics = listOf(
                        GattCharacteristicRecord(
                            uuid = "00002a2b-0000-1000-8000-00805f9b34fb",
                            properties = setOf("READ", "NOTIFY", "WRITE")
                        )
                    )
                )
            ),
            capturedAtMillis = 100L
        )
        val finding = snapshot.findings.firstOrNull { it.id == "standard:1805" }
        assertNotNull(finding)
        assertTrue(finding?.detail?.contains("sincronização padrão habilitável") == true)
    }

    @Test
    fun proprietaryWriteCharacteristicsAreOnlyCataloguedAsBlocked() {
        val snapshot = FeatureDiscoveryEngine.captureInventory(
            listOf(
                GattServiceRecord(
                    uuid = "12345678-1234-5678-1234-56789abcdef0",
                    characteristics = listOf(
                        GattCharacteristicRecord(
                            uuid = "12345678-1234-5678-1234-56789abcdef1",
                            properties = setOf("WRITE", "WRITE_NO_RESPONSE")
                        )
                    )
                )
            ),
            capturedAtMillis = 100L
        )
        val finding = snapshot.findings.firstOrNull { it.id.startsWith("blocked-write:") }
        assertNotNull(finding)
        assertEquals("BLOQUEADO • NENHUM COMANDO ENVIADO", finding?.status)
    }

    @Test
    fun nusRxIsAlwaysMarkedAsBlockedAndNeverAsAnEnabledFeature() {
        val snapshot = FeatureDiscoveryEngine.captureInventory(
            listOf(
                GattServiceRecord(
                    uuid = "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
                    characteristics = listOf(
                        GattCharacteristicRecord(
                            uuid = "6e400002-b5a3-f393-e0a9-e50e24dcca9e",
                            properties = setOf("WRITE_NO_RESPONSE")
                        ),
                        GattCharacteristicRecord(
                            uuid = "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
                            properties = setOf("NOTIFY")
                        )
                    )
                )
            ),
            capturedAtMillis = 100L
        )
        val rx = snapshot.findings.firstOrNull { it.id == "transport:nus-rx" }
        val tx = snapshot.findings.firstOrNull { it.id == "transport:nus-tx" }
        assertEquals("ESCRITA BLOQUEADA", rx?.status)
        assertNotNull(tx)
        assertFalse(tx?.status?.contains("ESCRITA") == true)
    }

    @Test
    fun notificationObservationStoresMetadataAndCandidateWithoutRawPayload() {
        val updated = FeatureDiscoveryEngine.observeNotification(
            previous = FeatureDiscoveryEngine.emptySnapshot(100L),
            serviceUuid = "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
            characteristicUuid = "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
            payloadBytes = 8,
            observedAtMillis = 200L,
            frameCommand = 0x91,
            frameResponse = 0x80,
            batteryCandidatePercent = 72
        )
        assertEquals(1, updated.traffic.size)
        assertEquals(1, updated.traffic.first().notificationCount)
        assertEquals(8, updated.traffic.first().lastPayloadBytes)
        assertEquals(72, updated.traffic.first().batteryCandidatePercent)
        val candidate = updated.findings.firstOrNull { it.id == "candidate:battery" }
        assertEquals("CANDIDATO • NÃO VALIDADO", candidate?.status)
        assertFalse(updated.findings.any { it.detail.contains("AB-00-05-FF-91-80-00-48") })
    }

    @Test
    fun observedNusTxTrafficUpdatesTheNusFeatureState() {
        val inventory = FeatureDiscoveryEngine.captureInventory(
            listOf(
                GattServiceRecord(
                    uuid = "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
                    characteristics = listOf(
                        GattCharacteristicRecord(
                            uuid = "6e400002-b5a3-f393-e0a9-e50e24dcca9e",
                            properties = setOf("WRITE_NO_RESPONSE")
                        ),
                        GattCharacteristicRecord(
                            uuid = "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
                            properties = setOf("NOTIFY")
                        )
                    )
                )
            ),
            capturedAtMillis = 100L
        )
        val updated = FeatureDiscoveryEngine.observeNotification(
            previous = inventory,
            serviceUuid = "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
            characteristicUuid = "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
            payloadBytes = 8,
            observedAtMillis = 200L
        )
        val nus = updated.findings.firstOrNull { it.id == "transport:nus-tx" }
        assertEquals("TRÁFEGO RECEBIDO • SEM SEMÂNTICA TOTALMENTE VALIDADA", nus?.status)
        assertEquals(1, nus?.observationCount)
    }

    @Test
    fun unknownFrameIsRecordedWithoutGuessingItsFunction() {
        val updated = FeatureDiscoveryEngine.observeNotification(
            previous = FeatureDiscoveryEngine.emptySnapshot(100L),
            serviceUuid = "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
            characteristicUuid = "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
            payloadBytes = 12,
            observedAtMillis = 200L,
            frameCommand = 0x92,
            frameResponse = 0xC0
        )
        val finding = updated.findings.firstOrNull { it.id == "ab-frame:92:C0" }
        assertEquals("FORMATO OBSERVADO • SEM SEMÂNTICA CONFIRMADA", finding?.status)
    }
}
