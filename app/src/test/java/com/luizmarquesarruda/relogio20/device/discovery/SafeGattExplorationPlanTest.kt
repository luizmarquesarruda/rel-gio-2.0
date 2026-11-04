package com.luizmarquesarruda.relogio20.device.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeGattExplorationPlanTest {
    @Test
    fun plansOnlyKnownStandardReadsAndMeasurements() {
        val plan = SafeGattExplorationPlan.build(
            listOf(
                GattServiceRecord(
                    "0000180f-0000-1000-8000-00805f9b34fb",
                    listOf(GattCharacteristicRecord(
                        "00002a19-0000-1000-8000-00805f9b34fb",
                        setOf("READ", "NOTIFY"),
                        setOf("00002902-0000-1000-8000-00805f9b34fb")
                    ))
                )
            )
        )
        assertEquals(2, plan.size)
        assertTrue(plan.any { it.kind == SafeGattExplorationPlan.Kind.READ_STANDARD_VALUE })
        assertTrue(plan.any { it.kind == SafeGattExplorationPlan.Kind.SUBSCRIBE_STANDARD_MEASUREMENT })
    }

    @Test
    fun excludesSecureDfuAndUnknownProprietaryCharacteristics() {
        val plan = SafeGattExplorationPlan.build(
            listOf(
                GattServiceRecord(
                    "0000fe59-0000-1000-8000-00805f9b34fb",
                    listOf(GattCharacteristicRecord(
                        "8ec90001-f315-4f60-9fb8-838830daea50",
                        setOf("NOTIFY", "WRITE"),
                        setOf("00002902-0000-1000-8000-00805f9b34fb")
                    ))
                ),
                GattServiceRecord(
                    "00003802-0000-1000-8000-00805f9b34fb",
                    listOf(GattCharacteristicRecord(
                        "00004a02-0000-1000-8000-00805f9b34fb",
                        setOf("READ", "NOTIFY", "WRITE"),
                        setOf("00002902-0000-1000-8000-00805f9b34fb")
                    ))
                ),
                GattServiceRecord(
                    "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
                    listOf(GattCharacteristicRecord(
                        "6e400002-b5a3-f393-e0a9-e50e24dcca9e",
                        setOf("WRITE_NO_RESPONSE")
                    ))
                )
            )
        )
        assertTrue(plan.isEmpty())
    }

    @Test
    fun doesNotSubscribeWithoutCccd() {
        val plan = SafeGattExplorationPlan.build(
            listOf(
                GattServiceRecord(
                    "0000180f-0000-1000-8000-00805f9b34fb",
                    listOf(GattCharacteristicRecord(
                        "00002a19-0000-1000-8000-00805f9b34fb",
                        setOf("NOTIFY")
                    ))
                )
            )
        )
        assertTrue(plan.isEmpty())
    }
}
