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
    fun defaultPolicyHasNoAllowedCharacteristicWrites() {
        assertTrue(AccessPolicy.DEFAULT.allowWrite.isEmpty())
    }

    @Test
    fun currentTimeCharacteristicWriteIsRejectedEvenWithExplicitConsent() {
        val policy = AccessPolicy.DEFAULT
        assertFalse(policy.canWrite(
            AccessPolicy.CURRENT_TIME_SERVICE,
            AccessPolicy.CURRENT_TIME_CHARACTERISTIC,
            explicitUserConsent = false
        ))
        assertFalse(policy.canWrite(
            AccessPolicy.CURRENT_TIME_SERVICE,
            AccessPolicy.CURRENT_TIME_CHARACTERISTIC,
            explicitUserConsent = true
        ))
    }

    @Test
    fun hardBlocklistCannotBeMutatedAtRuntime() {
        @Suppress("UNCHECKED_CAST")
        val mutableView = AccessPolicy.HARD_FORBIDDEN as MutableSet<java.util.UUID>
        assertThrows(UnsupportedOperationException::class.java) {
            mutableView.add(java.util.UUID.randomUUID())
        }
    }
}
