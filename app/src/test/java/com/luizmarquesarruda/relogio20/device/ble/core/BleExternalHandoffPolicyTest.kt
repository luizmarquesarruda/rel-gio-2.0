package com.luizmarquesarruda.relogio20.device.ble.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BleExternalHandoffPolicyTest {
    @Test
    fun handoffBlocksAutomaticReconnectEvenWhenReconnectWasPreviouslyEnabled() {
        assertFalse(BleExternalHandoffPolicy.canAutoReconnect(handoffActive = true, reconnectEnabled = true))
    }

    @Test
    fun reconnectRemainsAvailableAfterUserEndsHandoff() {
        assertTrue(BleExternalHandoffPolicy.canAutoReconnect(handoffActive = false, reconnectEnabled = true))
    }

    @Test
    fun handoffBlocksInitialDiscoveryOnActivityResume() {
        assertFalse(BleExternalHandoffPolicy.shouldStartInitialDiscovery(handoffActive = true, initialDiscoveryPending = true))
    }

    @Test
    fun initialDiscoveryStillRunsWhenNoHandoffWasRequested() {
        assertTrue(BleExternalHandoffPolicy.shouldStartInitialDiscovery(handoffActive = false, initialDiscoveryPending = true))
    }

    @Test
    fun handoffBlocksManualAndAutomaticScanEntryPoints() {
        assertFalse(BleExternalHandoffPolicy.canStartBleScan(handoffActive = true))
        assertTrue(BleExternalHandoffPolicy.canStartBleScan(handoffActive = false))
    }
}
