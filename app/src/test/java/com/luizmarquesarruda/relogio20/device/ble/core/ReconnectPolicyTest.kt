package com.luizmarquesarruda.relogio20.device.ble.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ReconnectPolicyTest {
    @Test
    fun buildConfigTestModeHasOneAttempt() {
        assertEquals(1, ReconnectPolicy.fromBuildConfig(true).maxAttempts)
    }

    @Test
    fun productionPolicyHasBoundedRetries() {
        val policy = ReconnectPolicy.fromBuildConfig(false)
        assertEquals(3, policy.maxAttempts)
        assertEquals(1_000L, policy.initialDelayMs)
    }
}
