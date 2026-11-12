package com.luizmarquesarruda.relogio20.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {
    @Test
    fun startWhileActiveFinalizesPreviousAsOrphaned() {
        val manager = SessionManager(10_000L)
        manager.start("first", 100L)
        manager.start("second", 200L)
        assertEquals(SessionEndReason.ORPHANED, manager.finishedSessions().first().endReason)
    }

    @Test
    fun watchdogFinalizesStalledSession() {
        val manager = SessionManager(1_000L)
        manager.start("session", 100L)
        assertFalse(manager.watchdogTick(1_100L))
        assertTrue(manager.watchdogTick(1_101L))
        assertEquals(SessionEndReason.STALLED, manager.finishedSessions().single().endReason)
    }

    @Test
    fun finalizeIsIdempotent() {
        val manager = SessionManager(10_000L)
        manager.start("session", 100L)
        assertTrue(manager.finalize(SessionEndReason.COMPLETED, 200L))
        assertFalse(manager.finalize(SessionEndReason.COMPLETED, 300L))
        assertEquals(1, manager.finishedSessions().size)
    }
}
