package com.luizmarquesarruda.relogio20.device.ble.gatt

import org.junit.Assert.assertEquals
import org.junit.Test

class InstanceIdDedupTest {
    @Test
    fun duplicateUuidSameInstanceCollapsesButDistinctInstancesRemain() {
        val rows = listOf(
            GattDescriptorInventoryRow("char-a", "2902", 0),
            GattDescriptorInventoryRow("char-a", "2902", 0),
            GattDescriptorInventoryRow("char-a", "2902", 1)
        )
        assertEquals(listOf(0, 1), GattInventoryLogger.deduplicate(rows).map { it.instanceId })
    }
}
