package com.luizmarquesarruda.relogio20.data.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryTestSessionTest {
    @Test
    fun countdownUsesDeadlineAndRoundsUpToWholeSeconds() {
        val session = DiscoveryTestSession("s", 1_000L, 61_000L, 60)
        assertEquals(60, session.remainingSeconds(1_000L))
        assertEquals(2, session.remainingSeconds(59_001L))
        assertEquals(1, session.remainingSeconds(60_001L))
        assertEquals(0, session.remainingSeconds(61_000L))
    }

    @Test
    fun sessionIsInactiveAtItsDeadline() {
        val session = DiscoveryTestSession("s", 1_000L, 61_000L, 60)
        assertTrue(session.isActive(60_999L))
        assertFalse(session.isActive(61_000L))
    }

    @Test
    fun onlySupportedDurationsAreAccepted() {
        assertTrue(DiscoveryTestDuration.isAllowed(60))
        assertTrue(DiscoveryTestDuration.isAllowed(120))
        assertTrue(DiscoveryTestDuration.isAllowed(300))
        assertTrue(DiscoveryTestDuration.isAllowed(600))
        assertFalse(DiscoveryTestDuration.isAllowed(90))
    }
}
