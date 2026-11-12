package com.luizmarquesarruda.relogio20.domain.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class StepsSnapshotTest {
    private val start = Instant.parse("2026-11-04T00:00:00Z")
    private val end = Instant.parse("2026-11-04T12:00:00Z")
    private val received = Instant.parse("2026-11-04T12:00:01Z")

    @Test
    fun zeroStepsIsARealAggregateAndNotMissingData() {
        val snapshot = StepsSnapshot(
            totalSteps = 0L,
            intervalStart = start,
            intervalEnd = end,
            receivedAt = received
        )

        assertTrue(snapshot.hasRecords)
        assertEquals(0L, snapshot.totalSteps)
    }

    @Test
    fun nullStepsPreservesAbsenceOfRecords() {
        val snapshot = StepsSnapshot(
            totalSteps = null,
            intervalStart = start,
            intervalEnd = end,
            receivedAt = received
        )

        assertFalse(snapshot.hasRecords)
        assertNull(snapshot.totalSteps)
    }

    @Test
    fun healthConnectAggregateOriginIsPreserved() {
        val snapshot = StepsSnapshot(
            totalSteps = 1250L,
            intervalStart = start,
            intervalEnd = end,
            receivedAt = received
        )

        assertEquals(HealthDataOrigin.HEALTH_CONNECT_AGGREGATE, snapshot.origin)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeStepsAreRejected() {
        StepsSnapshot(
            totalSteps = -1L,
            intervalStart = start,
            intervalEnd = end,
            receivedAt = received
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun reversedIntervalIsRejected() {
        StepsSnapshot(
            totalSteps = 1L,
            intervalStart = end,
            intervalEnd = start,
            receivedAt = received
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsReceiptBeforeQueriedIntervalEnds() {
        StepsSnapshot(
            totalSteps = 1L,
            intervalStart = start,
            intervalEnd = end,
            receivedAt = end.minusSeconds(1)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonHealthConnectOriginIsRejected() {
        StepsSnapshot(
            totalSteps = 1L,
            intervalStart = start,
            intervalEnd = end,
            receivedAt = received,
            origin = HealthDataOrigin.GT3_MAX
        )
    }
}
