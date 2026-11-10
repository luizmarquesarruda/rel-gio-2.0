package com.luizmarquesarruda.relogio20.domain.weather

/**
 * Keeps the periodic six-hour job from racing the direct refresh performed
 * when a city is selected or the weather screen is reopened.
 */
internal object WeatherRefreshPolicy {
    const val PERIODIC_INTERVAL_HOURS = 6L
    const val MIN_REFRESH_AGE_MILLIS = 4L * 60L * 60L * 1_000L + 30L * 60L * 1_000L

    fun shouldSkipPeriodicRefresh(
        lastFetchedAtMillis: Long?,
        nowMillis: Long,
    ): Boolean {
        if (lastFetchedAtMillis == null || lastFetchedAtMillis <= 0L) return false
        val ageMillis = nowMillis - lastFetchedAtMillis
        return ageMillis >= 0L && ageMillis < MIN_REFRESH_AGE_MILLIS
    }
}
