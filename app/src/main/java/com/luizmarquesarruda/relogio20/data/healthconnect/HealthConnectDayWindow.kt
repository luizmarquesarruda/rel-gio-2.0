package com.luizmarquesarruda.relogio20.data.healthconnect

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class HealthConnectDayInterval(val start: Instant, val end: Instant)

/** Calendar-day calculations use the caller's time zone and remain compatible with minSdk 26. */
internal object HealthConnectDayWindow {
    fun startOfDay(now: Instant, zoneId: ZoneId): Instant =
        now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()

    /** Returns null for today, future dates, or any interval not fully elapsed. */
    fun completedDay(day: LocalDate, now: Instant, zoneId: ZoneId): HealthConnectDayInterval? {
        val today = now.atZone(zoneId).toLocalDate()
        if (!day.isBefore(today)) return null

        val start = day.atStartOfDay(zoneId).toInstant()
        val end = day.plusDays(1).atStartOfDay(zoneId).toInstant()
        return if (end.isAfter(now)) null else HealthConnectDayInterval(start, end)
    }
}
