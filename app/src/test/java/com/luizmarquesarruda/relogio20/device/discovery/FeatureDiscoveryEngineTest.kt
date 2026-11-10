package com.luizmarquesarruda.relogio20.device.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureDiscoveryEngineTest {
    @Test
    fun findsStandardHeartRateProfileWithoutClaimingMeasurementIsValidated() {
        val snapshot =
            FeatureDiscoveryEngine.captureInventory(
                listOf(
                    GattServiceRecord(
                        uuid = "0000180d-0000-1000-8000-00805f9b34fb",
                        characteristics =
                            listOf(
                                GattCharacteristicRecord(
                                    uuid = "00002a37-0000-1000-8000-00805f9b34fb",
                                    properties = setOf("NOTIFY"),
                                ),
                            ),
                    ),
                ),
                capturedAtMillis = 100L,
            )
        val finding = snapshot.findings.firstOrNull { it.id == "standard:180d" }
        assertNotNull(finding)
        assertTrue(finding?.status?.contains("NÃO VALIDADOS") == true)
        assertTrue(finding?.detail?.contains("Nenhuma leitura") == true)
    }

    @Test
    fun detectsStandardCurrentTimeCharacteristicAndItsWriteCapability() {
        val snapshot =
            FeatureDiscoveryEngine.captureInventory(
                listOf(
                    GattServiceRecord(
                        uuid = "00001805-0000-1000-8000-00805f9b34fb",
                        characteristics =
                            listOf(
                                GattCharacteristicRecord(
                                    uuid = "00002a2b-0000-1000-8000-00805f9b34fb",
                                    properties = setOf("READ", "NOTIFY", "WRITE"),
                                ),
                            ),
                    ),
                ),
                capturedAtMillis = 100L,
            )
        val finding = snapshot.findings.firstOrNull { it.id == "standard:1805" }
        assertNotNull(finding)
        assertTrue(finding?.detail?.contains("escrita CTS suspensa pela política atual") == true)
    }

    @Test
    fun proprietaryWriteCharacteristicsAreOnlyCataloguedAsBlocked() {
        val snapshot =
            FeatureDiscoveryEngine.captureInventory(
                listOf(
                    GattServiceRecord(
                        uuid = "12345678-1234-5678-1234-56789abcdef0",
                        characteristics =
                            listOf(
                                GattCharacteristicRecord(
                                    uuid = "12345678-1234-5678-1234-56789abcdef1",
                                    properties = setOf("WRITE", "WRITE_NO_RESPONSE"),
                                ),
                            ),
                    ),
                ),
                capturedAtMillis = 100L,
            )
        val finding = snapshot.findings.firstOrNull { it.id.startsWith("blocked-write:") }
        assertNotNull(finding)
        assertEquals("BLOQUEADO • NENHUM COMANDO ENVIADO", finding?.status)
    }

    @Test
    fun nusRxIsAlwaysMarkedAsBlockedAndNeverAsAnEnabledFeature() {
        val snapshot =
            FeatureDiscoveryEngine.captureInventory(
                listOf(
                    GattServiceRecord(
                        uuid = "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
                        characteristics =
                            listOf(
                                GattCharacteristicRecord(
                                    uuid = "6e400002-b5a3-f393-e0a9-e50e24dcca9e",
                                    properties = setOf("WRITE_NO_RESPONSE"),
                                ),
                                GattCharacteristicRecord(
                                    uuid = "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
                                    properties = setOf("NOTIFY"),
                                ),
                            ),
                    ),
                ),
                capturedAtMillis = 100L,
            )
        val rx = snapshot.findings.firstOrNull { it.id == "transport:nus-rx" }
        val tx = snapshot.findings.firstOrNull { it.id == "transport:nus-tx" }
        assertEquals("ESCRITA BLOQUEADA", rx?.status)
        assertNotNull(tx)
        assertFalse(tx?.status?.contains("ESCRITA") == true)
    }

    @Test
    fun notificationObservationStoresMetadataAndCandidateWithoutRawPayload() {
        val updated =
            FeatureDiscoveryEngine.observeNotification(
                previous = FeatureDiscoveryEngine.emptySnapshot(100L),
                serviceUuid = "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
                characteristicUuid = "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
                payloadBytes = 8,
                observedAtMillis = 200L,
                frameCommand = 0x91,
                frameResponse = 0x80,
                batteryCandidatePercent = 72,
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
        val inventory =
            FeatureDiscoveryEngine.captureInventory(
                listOf(
                    GattServiceRecord(
                        uuid = "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
                        characteristics =
                            listOf(
                                GattCharacteristicRecord(
                                    uuid = "6e400002-b5a3-f393-e0a9-e50e24dcca9e",
                                    properties = setOf("WRITE_NO_RESPONSE"),
                                ),
                                GattCharacteristicRecord(
                                    uuid = "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
                                    properties = setOf("NOTIFY"),
                                ),
                            ),
                    ),
                ),
                capturedAtMillis = 100L,
            )
        val updated =
            FeatureDiscoveryEngine.observeNotification(
                previous = inventory,
                serviceUuid = "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
                characteristicUuid = "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
                payloadBytes = 8,
                observedAtMillis = 200L,
            )
        val nus = updated.findings.firstOrNull { it.id == "transport:nus-tx" }
        assertEquals("TRÁFEGO RECEBIDO • SEM SEMÂNTICA TOTALMENTE VALIDADA", nus?.status)
        assertEquals(1, nus?.observationCount)
    }

    @Test
    fun unknownFrameIsRecordedWithoutGuessingItsFunction() {
        val updated =
            FeatureDiscoveryEngine.observeNotification(
                previous = FeatureDiscoveryEngine.emptySnapshot(100L),
                serviceUuid = "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
                characteristicUuid = "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
                payloadBytes = 12,
                observedAtMillis = 200L,
                frameCommand = 0x92,
                frameResponse = 0xC0,
            )
        val finding = updated.findings.firstOrNull { it.id == "ab-frame:92:C0" }
        assertEquals("FORMATO OBSERVADO • SEM SEMÂNTICA CONFIRMADA", finding?.status)
    }

    @Test
    fun inventoryStoresStructuralGattSignatureWithoutDeviceIdentifiers() {
        val services =
            listOf(
                GattServiceRecord(
                    "0000180f-0000-1000-8000-00805f9b34fb",
                    listOf(
                        GattCharacteristicRecord(
                            "00002a19-0000-1000-8000-00805f9b34fb",
                            setOf("READ"),
                        ),
                    ),
                ),
            )
        val snapshot = FeatureDiscoveryEngine.captureInventory(services, 100L)
        val finding = snapshot.findings.firstOrNull { it.id == "firmware:gatt-signature" }

        assertNotNull(finding)
        assertEquals("BASELINE GATT REGISTRADO", finding?.status)
        val evidence = finding?.evidence.orEmpty()
        assertTrue(evidence.contains("sha256="))
        assertTrue(evidence.contains("sem MAC, serial, valores do fabricante ou payloads."))

        // The privacy note may name identifier categories; the evidence must never
        // contain an actual address or a serial value.
        assertFalse(Regex("(?i)(?:[0-9a-f]{2}:){5}[0-9a-f]{2}").containsMatchIn(evidence))
        assertFalse(Regex("(?i)(?:serial|s/n)\\s*[:=]\\s*\\S+").containsMatchIn(evidence))
    }

    @Test
    fun repeatedInventoryDetectsStableAndChangedGattStructure() {
        val original =
            listOf(
                GattServiceRecord(
                    "0000180f-0000-1000-8000-00805f9b34fb",
                    listOf(
                        GattCharacteristicRecord(
                            "00002a19-0000-1000-8000-00805f9b34fb",
                            setOf("READ"),
                        ),
                    ),
                ),
            )
        val first = FeatureDiscoveryEngine.captureInventory(original, 100L)
        val same = FeatureDiscoveryEngine.captureInventory(original, 200L, first)
        val changed =
            FeatureDiscoveryEngine.captureInventory(
                listOf(
                    GattServiceRecord(
                        "0000180f-0000-1000-8000-00805f9b34fb",
                        listOf(
                            GattCharacteristicRecord(
                                "00002a19-0000-1000-8000-00805f9b34fb",
                                setOf("READ", "NOTIFY"),
                            ),
                        ),
                    ),
                ),
                300L,
                same,
            )

        assertEquals(
            "ASSINATURA GATT INALTERADA",
            same.findings.first { it.id == "firmware:gatt-signature" }.status,
        )
        assertEquals(
            "ASSINATURA GATT ALTERADA • REVISAR",
            changed.findings.first { it.id == "firmware:gatt-signature" }.status,
        )
    }

    @Test
    fun standardCapabilityMatrixIsPersistedAndLiveTrafficRemainsUnvalidated() {
        val serviceUuid = "0000180d-0000-1000-8000-00805f9b34fb"
        val characteristicUuid = "00002a37-0000-1000-8000-00805f9b34fb"
        val inventory =
            FeatureDiscoveryEngine.captureInventory(
                listOf(
                    GattServiceRecord(
                        serviceUuid,
                        listOf(
                            GattCharacteristicRecord(
                                characteristicUuid,
                                setOf("NOTIFY"),
                                setOf("00002902-0000-1000-8000-00805f9b34fb"),
                            ),
                        ),
                    ),
                ),
                100L,
            )
        val capability = inventory.findings.firstOrNull { it.id == "capability:heart_rate" }
        assertEquals(
            GattCapabilityMatrix.State.NOTIFICATION_AVAILABLE.label,
            capability?.status,
        )

        val updated =
            FeatureDiscoveryEngine.observeNotification(
                previous = inventory,
                serviceUuid = serviceUuid,
                characteristicUuid = characteristicUuid,
                payloadBytes = 2,
                observedAtMillis = 200L,
            )
        val observed = updated.findings.firstOrNull { it.id == "capability:heart_rate" }
        assertEquals(GattCapabilityMatrix.State.TRAFFIC_OBSERVED.label, observed?.status)
        assertTrue(observed?.detail?.contains("sem validação semântica") == true)
    }
}
