package com.luizmarquesarruda.relogio20.domain.telemetry

import org.junit.Assert.assertTrue
import org.junit.Test

class WatchdogTest {
    @Test
    fun confirmedCccdWithoutNotificationsBecomesStalled() {
        val watchdog = TelemetryWatchdog(20_000L)
        val state = watchdog.tick(TelemetryState.Subscribed(100L), 20_101L)
        assertTrue(state is TelemetryState.Stalled)
    }
}
