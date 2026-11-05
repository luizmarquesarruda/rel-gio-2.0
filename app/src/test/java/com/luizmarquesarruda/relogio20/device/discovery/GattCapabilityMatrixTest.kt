package com.luizmarquesarruda.relogio20.device.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GattCapabilityMatrixTest {
    @Test
    fun absentSigServiceIsNotReportedAsPhysicallyAbsentSensor() {
        val assessment = GattCapabilityMatrix.evaluate(emptyList())
            .single { it.id == "heart_rate" }

        assertEquals(GattCapabilityMatrix.State.NOT_ADVERTISED, assessment.state)
        assertTrue(assessment.detail.contains("não prova ausência física"))
        assertEquals(0, assessment.observationCount)
    }

    @Test
    fun serviceWithoutExpectedCharacteristicIsDistinguishedFromMissingService() {
        val assessment = GattCapabilityMatrix.evaluate(
            listOf(GattServiceRecord("0000180d-0000-1000-8000-00805f9b34fb", emptyList()))
        ).single { it.id == "heart_rate" }

        assertEquals(
            GattCapabilityMatrix.State.SERVICE_WITHOUT_CHARACTERISTIC,
            assessment.state
        )
    }

    @Test
    fun readAndNotifyAreReportedOnlyWhenStandardCccdIsPresent() {
        val assessment = GattCapabilityMatrix.evaluate(
            listOf(
                GattServiceRecord(
                    "0000180d-0000-1000-8000-00805f9b34fb",
                    listOf(
                        GattCharacteristicRecord(
                            "00002a37-0000-1000-8000-00805f9b34fb",
                            setOf("READ", "NOTIFY"),
                            setOf("00002902-0000-1000-8000-00805f9b34fb")
                        )
                    )
                )
            )
        ).single { it.id == "heart_rate" }

        assertEquals(
            GattCapabilityMatrix.State.READ_AND_NOTIFICATION_AVAILABLE,
            assessment.state
        )
        assertTrue(assessment.evidence.contains("2a37"))
    }

    @Test
    fun notificationPropertyWithoutCccdIsNotConsideredSubscribable() {
        val assessment = GattCapabilityMatrix.evaluate(
            listOf(
                GattServiceRecord(
                    "0000180d-0000-1000-8000-00805f9b34fb",
                    listOf(
                        GattCharacteristicRecord(
                            "00002a37-0000-1000-8000-00805f9b34fb",
                            setOf("NOTIFY")
                        )
                    )
                )
            )
        ).single { it.id == "heart_rate" }

        assertEquals(
            GattCapabilityMatrix.State.NOTIFICATION_WITHOUT_CCCD,
            assessment.state
        )
        assertTrue(assessment.detail.contains("sem CCCD"))
    }

    @Test
    fun readAndNotifyWithoutCccdIsNotReportedAsSubscribable() {
        val assessment = GattCapabilityMatrix.evaluate(
            listOf(
                GattServiceRecord(
                    "0000180d-0000-1000-8000-00805f9b34fb",
                    listOf(
                        GattCharacteristicRecord(
                            "00002a37-0000-1000-8000-00805f9b34fb",
                            setOf("READ", "NOTIFY")
                        )
                    )
                )
            )
        ).single { it.id == "heart_rate" }

        assertEquals(
            GattCapabilityMatrix.State.READ_AND_NOTIFICATION_WITHOUT_CCCD,
            assessment.state
        )
        assertTrue(assessment.detail.contains("sem CCCD"))
    }

    @Test
    fun notificationHistoryIsVisibleButRemainsUnvalidated() {
        val assessment = GattCapabilityMatrix.evaluate(
            listOf(
                GattServiceRecord(
                    "0000180d-0000-1000-8000-00805f9b34fb",
                    listOf(
                        GattCharacteristicRecord(
                            "00002a37-0000-1000-8000-00805f9b34fb",
                            setOf("NOTIFY"),
                            setOf("00002902-0000-1000-8000-00805f9b34fb")
                        )
                    )
                )
            ),
            listOf(
                CharacteristicTraffic(
                    serviceUuid = "0000180d-0000-1000-8000-00805f9b34fb",
                    characteristicUuid = "00002a37-0000-1000-8000-00805f9b34fb",
                    notificationCount = 3,
                    lastSeenAtMillis = 200L,
                    lastPayloadBytes = 4
                )
            )
        ).single { it.id == "heart_rate" }

        assertEquals(GattCapabilityMatrix.State.HISTORICAL_TRAFFIC, assessment.state)
        assertEquals(3, assessment.observationCount)
        assertTrue(assessment.state.label.contains("NÃO VALIDADO"))
    }

    @Test
    fun featureLookupUsesServiceAndCharacteristicTogether() {
        assertEquals(
            "heart_rate",
            GattCapabilityMatrix.featureIdFor(
                "0000180d-0000-1000-8000-00805f9b34fb",
                "00002a37-0000-1000-8000-00805f9b34fb"
            )
        )
        assertFalse(
            GattCapabilityMatrix.featureIdFor(
                "12345678-1234-5678-1234-56789abcdef0",
                "00002a37-0000-1000-8000-00805f9b34fb"
            ) != null
        )
    }
}
