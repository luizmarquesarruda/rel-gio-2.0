package com.luizmarquesarruda.relogio20.data.healthconnect

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.Duration

class HealthConnectDayWindowTest {
    @Test
    fun startOfDayUsesCalendarDateInRequestedZone() {
        val now = Instant.parse("2026-11-04T02:30:00Z")
        assertEquals(
            Instant.parse("2026-11-03T03:00:00Z"),
            HealthConnectDayWindow.startOfDay(now, ZoneId.of("America/Sao_Paulo"))
        )
    }

    @Test
    fun startOfDayRespectsNextCalendarDateAcrossPositiveUtcOffset() {
        val now = Instant.parse("2026-11-04T11:30:00Z")
        assertEquals(
            Instant.parse("2026-11-04T11:00:00Z"),
            HealthConnectDayWindow.startOfDay(now, ZoneId.of("Pacific/Auckland"))
        )
    }

    @Test
    fun completedDayRejectsTodayAndFutureDates() {
        val now = Instant.parse("2026-11-10T15:00:00Z")
        val zone = ZoneId.of("America/Sao_Paulo")
        assertEquals(null, HealthConnectDayWindow.completedDay(LocalDate.of(2026, 11, 10), now, zone))
        assertEquals(null, HealthConnectDayWindow.completedDay(LocalDate.of(2026, 11, 11), now, zone))
    }

    @Test
    fun completedDayUsesCalendarBoundariesAcrossDstTransition() {
        val zone = ZoneId.of("America/New_York")
        val spring = HealthConnectDayWindow.completedDay(
            LocalDate.of(2026, 3, 8), Instant.parse("2026-03-09T12:00:00Z"), zone
        )!!
        assertEquals(23L * 60L * 60L, Duration.between(spring.start, spring.end).seconds)

        val fall = HealthConnectDayWindow.completedDay(
            LocalDate.of(2026, 11, 1), Instant.parse("2026-11-02T12:00:00Z"), zone
        )!!
        assertEquals(25L * 60L * 60L, Duration.between(fall.start, fall.end).seconds)
    }
}
