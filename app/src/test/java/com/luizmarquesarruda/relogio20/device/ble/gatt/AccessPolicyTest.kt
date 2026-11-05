package com.luizmarquesarruda.relogio20.device.ble.gatt

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessPolicyTest {
    @Test
    fun forbiddenFe59WriteIsRejectedDuringPolicyConstruction() {
        assertThrows(IllegalArgumentException::class.java) {
            AccessPolicy(allowWrite = setOf(AccessPolicy.FE59_DFU_SERVICE))
        }
    }

    @Test
    fun dfuAndNusRxAreNeverWriteEligible() {
        val policy = AccessPolicy.DEFAULT
        assertFalse(policy.canWrite(AccessPolicy.FE59_DFU_SERVICE, AccessPolicy.DFU_CONTROL_POINT, true))
        assertFalse(policy.canWrite(AccessPolicy.NUS_SERVICE, AccessPolicy.NUS_RX, true))
    }

    @Test
    fun hardBlocklistCannotBeMutatedAtRuntime() {
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (AccessPolicy.HARD_FORBIDDEN as MutableSet<java.util.UUID>)
                .add(java.util.UUID.randomUUID())
        }
    }

    @Test
    fun onlyExplicitlyConfirmedSigCurrentTimeWriteIsPermitted() {
        val policy = AccessPolicy.DEFAULT
        assertFalse(policy.canWrite(
            AccessPolicy.CURRENT_TIME_SERVICE,
            AccessPolicy.CURRENT_TIME_CHARACTERISTIC,
            explicitUserConsent = false
        ))
        assertTrue(policy.canWrite(
            AccessPolicy.CURRENT_TIME_SERVICE,
            AccessPolicy.CURRENT_TIME_CHARACTERISTIC,
            explicitUserConsent = true
        ))
    }
}
