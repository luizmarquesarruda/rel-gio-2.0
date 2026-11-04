package com.luizmarquesarruda.relogio20.device.discovery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticGattDiscoveryPolicyTest {
    @Test
    fun requiresExplicitConsentAndFullFingerprint() {
        assertFalse(AutomaticGattDiscoveryPolicy.shouldStart(false, true, false))
        assertFalse(AutomaticGattDiscoveryPolicy.shouldStart(true, false, false))
        assertTrue(AutomaticGattDiscoveryPolicy.shouldStart(true, true, false))
    }

    @Test
    fun runsOnlyOncePerGattConnection() {
        assertFalse(AutomaticGattDiscoveryPolicy.shouldStart(true, true, true))
    }
}
