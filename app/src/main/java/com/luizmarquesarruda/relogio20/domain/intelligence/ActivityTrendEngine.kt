package com.luizmarquesarruda.relogio20.domain.intelligence

import com.luizmarquesarruda.relogio20.domain.health.HealthDataOrigin
import kotlin.math.abs

enum class ActivityTrendStatus {
    INSUFFICIENT_DATA, MIXED_SOURCES, INSUFFICIENT_BASELINE,
    NO_SIGNIFICANT_CHANGE, SIGNIFICANT_CHANGE
}
enum class ActivityTrendDirection { INCREASED, DECREASED }
enum class ActivityTrendSeverity { INFORMATION, ATTENTION }

data class ActivityTrendInsight(
    val direction: ActivityTrendDirection,
    val severity: ActivityTrendSeverity,
    val baselineAverageSteps: Double,
    val recentAverageSteps: Double,
    val percentageChange: Double,
    val origin: HealthDataOrigin,
    val firstEpochDay: Long,
    val lastEpochDay: Long
)

data class ActivityTrendAnalysis(
    val status: ActivityTrendStatus,
    val insight: ActivityTrendInsight? = null
) {
    init {
        require((status == ActivityTrendStatus.SIGNIFICANT_CHANGE) == (insight != null)) {
            "Only a significant change may carry an insight."
        }
    }
}

/**
 * Offline deterministic rule engine: compares two complete seven-day windows.
 * It refuses mixed origins and only accepts GT3 MAX data after physical validation.
 */
class ActivityTrendEngine(
    private val significantChangeThresholdPercent: Double = 25.0
) {
    init {
        require(significantChangeThresholdPercent > 0.0 &&
            significantChangeThresholdPercent.isFinite()) {
            "Threshold must be finite and greater than zero."
        }
    }

    fun analyze(
        observations: List<DailyActivityObservation>,
        asOfEpochDay: Long
    ): ActivityTrendAnalysis {
        val firstExpectedDay = asOfEpochDay - 14L
        val lastExpectedDay = asOfEpochDay - 1L
        val window = observations.filter { it.epochDay in firstExpectedDay..lastExpectedDay }

        if (window.size != 14 || window.map { it.epochDay }.toSet().size != 14) {
            return ActivityTrendAnalysis(ActivityTrendStatus.INSUFFICIENT_DATA)
        }
        val ordered = window.sortedBy { it.epochDay }
        if (ordered.first().epochDay != firstExpectedDay ||
            ordered.last().epochDay != lastExpectedDay ||
            ordered.zipWithNext().any { (a, b) -> b.epochDay != a.epochDay + 1L }) {
            return ActivityTrendAnalysis(ActivityTrendStatus.INSUFFICIENT_DATA)
        }

        val origins = ordered.map { it.origin }.toSet()
        if (origins.size != 1) return ActivityTrendAnalysis(ActivityTrendStatus.MIXED_SOURCES)
        val origin = origins.single()
        if (origin == HealthDataOrigin.GT3_MAX &&
            ordered.any { it.validationStatus.name != "PHYSICALLY_VALIDATED" }) {
            return ActivityTrendAnalysis(ActivityTrendStatus.INSUFFICIENT_DATA)
        }

        val baseline = ordered.take(7).map { it.steps.toDouble() }.average()
        if (baseline <= 0.0) return ActivityTrendAnalysis(ActivityTrendStatus.INSUFFICIENT_BASELINE)
        val recent = ordered.takeLast(7).map { it.steps.toDouble() }.average()
        val change = ((recent - baseline) / baseline) * 100.0
        if (abs(change) < significantChangeThresholdPercent) {
            return ActivityTrendAnalysis(ActivityTrendStatus.NO_SIGNIFICANT_CHANGE)
        }

        val direction = if (change < 0.0) ActivityTrendDirection.DECREASED
            else ActivityTrendDirection.INCREASED
        return ActivityTrendAnalysis(
            status = ActivityTrendStatus.SIGNIFICANT_CHANGE,
            insight = ActivityTrendInsight(
                direction = direction,
                severity = if (direction == ActivityTrendDirection.DECREASED)
                    ActivityTrendSeverity.ATTENTION else ActivityTrendSeverity.INFORMATION,
                baselineAverageSteps = baseline,
                recentAverageSteps = recent,
                percentageChange = change,
                origin = origin,
                firstEpochDay = firstExpectedDay,
                lastEpochDay = lastExpectedDay
            )
        )
    }
}
