package com.luizmarquesarruda.relogio20.data.healthconnect

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class HealthConnectDayWindowTest {
    @Test
    fun startOfDayUsesCalendarDateInRequestedZone() {
        val now = Instant.parse("2026-11-04T02:30:00Z")
        val saoPaulo = ZoneId.of("America/Sao_Paulo")

        assertEquals(
            Instant.parse("2026-11-03T03:00:00Z"),
            HealthConnectDayWindow.startOfDay(now, saoPaulo),
        )
    }

    @Test
    fun startOfDayRespectsNextCalendarDateAcrossPositiveUtcOffset() {
        val now = Instant.parse("2026-11-04T11:30:00Z")
        val auckland = ZoneId.of("Pacific/Auckland")

        assertEquals(
            Instant.parse("2026-11-04T11:00:00Z"),
            HealthConnectDayWindow.startOfDay(now, auckland),
        )
    }
}
