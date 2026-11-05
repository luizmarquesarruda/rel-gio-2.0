package com.luizmarquesarruda.relogio20.data.discovery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WriteFailureTrackerTest {
    @Test
    fun reportsFailureAtNextFlushAndThenResets() {
        val tracker = WriteFailureTracker()

        tracker.record(false)

        assertTrue(tracker.consumeFailure())
        assertFalse(tracker.consumeFailure())
    }

    @Test
    fun preservesFailureAcrossLaterSuccessfulWrites() {
        val tracker = WriteFailureTracker()

        tracker.record(false)
        tracker.record(true)

        assertTrue(tracker.consumeFailure())
    }

    @Test
    fun successfulWritesDoNotReportFailure() {
        val tracker = WriteFailureTracker()

        tracker.record(true)

        assertFalse(tracker.consumeFailure())
    }
}
