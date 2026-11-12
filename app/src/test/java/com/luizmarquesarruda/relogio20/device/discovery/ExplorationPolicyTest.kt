package com.luizmarquesarruda.relogio20.device.discovery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplorationPolicyTest {
    @Test
    fun serializeAlwaysContainsFe59DfuAndNusRxBlocklist() {
        val text = ExplorationPolicy.failClosed().serialize()
        assertTrue(text.contains("0000fe59-0000-1000-8000-00805f9b34fb"))
        assertTrue(text.contains("8ec90001-f315-4f60-9fb8-838830daea50"))
        assertTrue(text.contains("6e400002-b5a3-f393-e0a9-e50e24dcca9e"))
    }

    @Test
    fun emptyAllowlistFailsClosedForUnknownCharacteristic() {
        assertFalse(
            ExplorationPolicy.failClosed().isEligible(
                "00003802-0000-1000-8000-00805f9b34fb",
                "00004a02-0000-1000-8000-00805f9b34fb",
                ExplorationPolicy.Operation.READ,
            ),
        )
    }

    @Test
    fun buildConfigPolicyNeverAllowsCharacteristicWrites() {
        val service =
            GattServiceRecord(
                "00003802-0000-1000-8000-00805f9b34fb",
                listOf(
                    GattCharacteristicRecord(
                        "00004a02-0000-1000-8000-00805f9b34fb",
                        setOf("READ", "WRITE"),
                    ),
                ),
            )
        val policy = ExplorationPolicy.forObservedInventory(listOf(service))
        assertTrue(
            policy.isEligible(
                service.uuid,
                service.characteristics.single().uuid,
                ExplorationPolicy.Operation.READ,
            ),
        )
        assertFalse(
            policy.isEligible(
                service.uuid,
                service.characteristics.single().uuid,
                ExplorationPolicy.Operation.WRITE,
            ),
        )
    }
}
