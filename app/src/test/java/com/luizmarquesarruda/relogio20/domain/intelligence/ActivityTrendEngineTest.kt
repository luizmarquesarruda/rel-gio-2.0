package com.luizmarquesarruda.relogio20.domain.intelligence

import com.luizmarquesarruda.relogio20.domain.health.HealthDataOrigin
import com.luizmarquesarruda.relogio20.domain.health.HealthValidationStatus
import org.junit.Assert.*
import org.junit.Test

class ActivityTrendEngineTest {
    private val engine = ActivityTrendEngine()
    private val asOfDay = 20L

    private fun observations(
        baseline: Long = 6000L,
        recent: Long = 6000L,
        origin: HealthDataOrigin = HealthDataOrigin.HEALTH_CONNECT_AGGREGATE
    ) = (6L..19L).map { day ->
        DailyActivityObservation(
            epochDay = day,
            steps = if (day <= 12L) baseline else recent,
            origin = origin,
            receivedAtEpochMillis = (day + 1L) * 86_400_000L,
            validationStatus = if (origin == HealthDataOrigin.GT3_MAX)
                HealthValidationStatus.PHYSICALLY_VALIDATED
            else HealthValidationStatus.UNVALIDATED
        )
    }

    @Test fun detectsDecreaseAsAttention() {
        val result = engine.analyze(observations(recent = 4000L), asOfDay)
        assertEquals(ActivityTrendStatus.SIGNIFICANT_CHANGE, result.status)
        assertEquals(ActivityTrendDirection.DECREASED, result.insight?.direction)
        assertEquals(ActivityTrendSeverity.ATTENTION, result.insight?.severity)
    }

    @Test fun detectsIncreaseAsInformation() {
        val result = engine.analyze(observations(recent = 8000L), asOfDay)
        assertEquals(ActivityTrendDirection.INCREASED, result.insight?.direction)
        assertEquals(ActivityTrendSeverity.INFORMATION, result.insight?.severity)
    }

    @Test fun ignoresChangeBelowThreshold() {
        assertEquals(
            ActivityTrendStatus.NO_SIGNIFICANT_CHANGE,
            engine.analyze(observations(recent = 7000L), asOfDay).status
        )
    }

    @Test fun requiresFourteenConsecutiveDays() {
        assertEquals(
            ActivityTrendStatus.INSUFFICIENT_DATA,
            engine.analyze(observations().drop(1), asOfDay).status
        )
    }

    @Test fun refusesMixedSources() {
        val mixed = observations().mapIndexed { index, item ->
            if (index == 0) item.copy(origin = HealthDataOrigin.PHONE) else item
        }
        assertEquals(ActivityTrendStatus.MIXED_SOURCES, engine.analyze(mixed, asOfDay).status)
    }

    @Test fun refusesUnvalidatedWatchObservationAtConstruction() {
        assertThrows(IllegalArgumentException::class.java) {
            DailyActivityObservation(
                epochDay = 1L, steps = 100L, origin = HealthDataOrigin.GT3_MAX,
                receivedAtEpochMillis = 86_400_000L,
                validationStatus = HealthValidationStatus.UNVALIDATED
            )
        }
    }

    @Test fun acceptsPhysicallyValidatedWatchObservations() {
        val result = engine.analyze(
            observations(origin = HealthDataOrigin.GT3_MAX, recent = 4000L), asOfDay
        )
        assertEquals(ActivityTrendStatus.SIGNIFICANT_CHANGE, result.status)
        assertEquals(HealthDataOrigin.GT3_MAX, result.insight?.origin)
    }

    @Test fun rejectsZeroBaseline() {
        assertEquals(
            ActivityTrendStatus.INSUFFICIENT_BASELINE,
            engine.analyze(observations(baseline = 0L), asOfDay).status
        )
    }

    @Test fun ignoresDataOutsideWindow() {
        val extra = DailyActivityObservation(
            epochDay = 2L, steps = 99999L,
            origin = HealthDataOrigin.HEALTH_CONNECT_AGGREGATE,
            receivedAtEpochMillis = 3L * 86_400_000L,
            validationStatus = HealthValidationStatus.UNVALIDATED
        )
        assertEquals(
            ActivityTrendStatus.NO_SIGNIFICANT_CHANGE,
            engine.analyze(observations() + extra, asOfDay).status
        )
    }
}
