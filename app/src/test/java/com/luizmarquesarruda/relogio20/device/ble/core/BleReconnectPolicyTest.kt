package com.luizmarquesarruda.relogio20.device.ble.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BleReconnectPolicyTest {
    @Test
    fun doesNotRetryBeforeTheFirstConnectionRequest() {
        val policy = BleReconnectPolicy()

        assertFalse(policy.isEnabled)
        assertNull(policy.nextRetryDelayMillis())
    }

    @Test
    fun explicitConnectionRequestEnablesBoundedRecoveryBeforeFirstSuccess() {
        val policy = BleReconnectPolicy()

        policy.markConnectionRequested()

        assertTrue(policy.isEnabled)
        assertEquals(1_000L, policy.nextRetryDelayMillis())
        assertEquals(2_000L, policy.nextRetryDelayMillis())
    }

    @Test
    fun increasesRetryDelayAndCapsAtThirtySeconds() {
        val policy = BleReconnectPolicy(initiallyEnabled = true)

        assertEquals(1_000L, policy.nextRetryDelayMillis())
        assertEquals(2_000L, policy.nextRetryDelayMillis())
        assertEquals(5_000L, policy.nextRetryDelayMillis())
        assertEquals(10_000L, policy.nextRetryDelayMillis())
        assertEquals(20_000L, policy.nextRetryDelayMillis())
        assertEquals(30_000L, policy.nextRetryDelayMillis())
        assertEquals(30_000L, policy.nextRetryDelayMillis())
        assertEquals(30_000L, policy.nextRetryDelayMillis())
    }

    @Test
    fun successfulConnectionResetsTheRetrySequenceAndEnablesRecovery() {
        val policy = BleReconnectPolicy()

        policy.markConnected()
        assertEquals(1_000L, policy.nextRetryDelayMillis())
        assertEquals(2_000L, policy.nextRetryDelayMillis())

        policy.markConnected()

        assertTrue(policy.isEnabled)
        assertEquals(1_000L, policy.nextRetryDelayMillis())
    }

    @Test
    fun persistedEnabledStateCanBeRestored() {
        val policy = BleReconnectPolicy(initiallyEnabled = true)

        assertTrue(policy.isEnabled)
        assertEquals(1_000L, policy.nextRetryDelayMillis())
    }

    @Test
    fun manualDisconnectDisablesPendingRetries() {
        val policy = BleReconnectPolicy(initiallyEnabled = true)

        assertEquals(1_000L, policy.nextRetryDelayMillis())
        policy.markManuallyDisconnected()

        assertFalse(policy.isEnabled)
        assertNull(policy.nextRetryDelayMillis())
    }
}
