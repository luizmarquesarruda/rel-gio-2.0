package com.luizmarquesarruda.relogio20.data.healthconnect

import java.time.Instant
import java.time.ZoneId

/**
 * Builds the start of the calendar day in the supplied time zone.
 *
 * Use Instant.atZone instead of LocalDate.ofInstant so the API remains
 * available on the app's Android minSdk (26) without relying on API 34.
 */
internal object HealthConnectDayWindow {
    fun startOfDay(now: Instant, zoneId: ZoneId): Instant =
        now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
}
