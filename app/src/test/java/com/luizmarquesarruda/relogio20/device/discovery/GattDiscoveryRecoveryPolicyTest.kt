package com.luizmarquesarruda.relogio20.device.discovery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GattDiscoveryRecoveryPolicyTest {
    @Test
    fun recoversWhenCurrentConnectionIsStillWaitingForCallback() {
        assertTrue(GattDiscoveryRecoveryPolicy.shouldRecover(true, true, false, false))
    }

    @Test
    fun doesNotRecoverAfterCallbackHasArrived() {
        assertFalse(GattDiscoveryRecoveryPolicy.shouldRecover(true, true, false, true))
    }

    @Test
    fun doesNotRecoverForStaleOrDisconnectedGatt() {
        assertFalse(GattDiscoveryRecoveryPolicy.shouldRecover(false, true, false, false))
        assertFalse(GattDiscoveryRecoveryPolicy.shouldRecover(true, false, false, false))
    }

    @Test
    fun doesNotRecoverDuringManualDisconnect() {
        assertFalse(GattDiscoveryRecoveryPolicy.shouldRecover(true, true, true, false))
    }
}
