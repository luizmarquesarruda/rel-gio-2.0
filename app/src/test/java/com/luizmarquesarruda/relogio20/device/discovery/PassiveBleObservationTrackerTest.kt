package com.luizmarquesarruda.relogio20.device.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PassiveBleObservationTrackerTest {
    @Test
    fun tracksSequenceIntervalsAndPerChannelCountersWithoutPayload() {
        val tracker = PassiveBleObservationTracker()
        val first = tracker.observe("service", "characteristic", 8, "fp-a", 1_000L)
        val second = tracker.observe("service", "characteristic", 4, "fp-b", 1_750L)
        val other = tracker.observe("service-2", "characteristic", 2, "fp-a", 1_800L)

        assertEquals(1L, first.sequence)
        assertNull(first.intervalSincePreviousMillis)
        assertEquals(2L, second.sequence)
        assertEquals(750L, second.intervalSincePreviousMillis)
        assertEquals(2L, second.channelNotificationCount)
        assertEquals(12L, second.channelByteCount)
        assertEquals(2, second.distinctFingerprints)
        assertEquals(3L, other.sequence)
        assertEquals(1L, other.channelNotificationCount)
    }

    @Test
    fun boundsChannelsAndFingerprintHistory() {
        val tracker = PassiveBleObservationTracker(maxChannels = 1, maxFingerprintsPerChannel = 2)
        tracker.observe("s", "c", 1, "a", 1L)
        tracker.observe("s", "c", 1, "b", 2L)
        val third = tracker.observe("s", "c", 1, "c", 3L)
        assertEquals(2, third.distinctFingerprints)

        val replacement = tracker.observe("s2", "c2", 1, "d", 4L)
        assertEquals(1L, replacement.channelNotificationCount)
        assertNull(replacement.intervalSincePreviousMillis)
    }

    @Test
    fun resetStartsANewSession() {
        val tracker = PassiveBleObservationTracker()
        tracker.observe("s", "c", 8, "fp", 1L)
        tracker.resetSession()
        val next = tracker.observe("s", "c", 8, "fp", 2L)
        assertEquals(1L, next.sequence)
        assertEquals(1L, next.channelNotificationCount)
        assertNull(next.intervalSincePreviousMillis)
    }
}
