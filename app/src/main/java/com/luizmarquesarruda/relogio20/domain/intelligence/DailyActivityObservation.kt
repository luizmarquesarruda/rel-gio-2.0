package com.luizmarquesarruda.relogio20.domain.intelligence

import com.luizmarquesarruda.relogio20.domain.health.HealthDataOrigin
import com.luizmarquesarruda.relogio20.domain.health.HealthValidationStatus

/** One complete daily step aggregate with explicit source provenance. */
data class DailyActivityObservation(
    val epochDay: Long,
    val steps: Long,
    val origin: HealthDataOrigin,
    val receivedAtEpochMillis: Long,
    val validationStatus: HealthValidationStatus
) {
    init {
        require(steps >= 0L) { "Daily steps cannot be negative." }
        require(receivedAtEpochMillis > 0L) { "Receive timestamp must be positive." }
        if (origin == HealthDataOrigin.GT3_MAX) {
            require(validationStatus == HealthValidationStatus.PHYSICALLY_VALIDATED) {
                "GT3 MAX activity must be physically validated before intelligence analysis."
            }
        }
    }
}
