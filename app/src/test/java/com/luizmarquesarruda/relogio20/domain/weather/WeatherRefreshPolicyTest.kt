package com.luizmarquesarruda.relogio20.domain.weather

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRefreshPolicyTest {
    @Test
    fun skipsWhenCachedForecastIsRecent() {
        val now = 10_000_000L
        assertTrue(WeatherRefreshPolicy.shouldSkipPeriodicRefresh(now - 30 * 60_000L, now))
    }

    @Test
    fun refreshesWhenCacheIsOlderThanGuardWindow() {
        val now = 10_000_000L
        assertFalse(
            WeatherRefreshPolicy.shouldSkipPeriodicRefresh(
                now - WeatherRefreshPolicy.MIN_REFRESH_AGE_MILLIS,
                now,
            ),
        )
    }

    @Test
    fun refreshesWhenThereIsNoCacheOrClockMovedBackwards() {
        val now = 10_000_000L
        assertFalse(WeatherRefreshPolicy.shouldSkipPeriodicRefresh(null, now))
        assertFalse(WeatherRefreshPolicy.shouldSkipPeriodicRefresh(now + 1_000L, now))
    }
}
