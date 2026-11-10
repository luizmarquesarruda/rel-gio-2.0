package com.luizmarquesarruda.relogio20.device.discovery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomaticCaptureSessionPolicyTest {
    @Test
    fun automaticModeCanFinishOnlyItsOwnActiveSession() {
        assertTrue(
            AutomaticCaptureSessionPolicy.shouldFinish(
                sessionMatches = true,
                createdByAutomaticMode = true,
            ),
        )
        assertFalse(
            AutomaticCaptureSessionPolicy.shouldFinish(
                sessionMatches = true,
                createdByAutomaticMode = false,
            ),
        )
        assertFalse(
            AutomaticCaptureSessionPolicy.shouldFinish(
                sessionMatches = false,
                createdByAutomaticMode = true,
            ),
        )
    }
}
