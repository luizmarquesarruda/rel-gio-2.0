package com.luizmarquesarruda.relogio20.domain.health

import java.time.Instant

/**
 * Snapshot of an aggregate returned by Health Connect.
 *
 * A null total means that the provider returned no aggregate value for the interval;
 * zero is a real aggregate value and must not be conflated with missing data.
 */
data class StepsSnapshot(
    val totalSteps: Long?,
    val intervalStart: Instant,
    val intervalEnd: Instant,
    val receivedAt: Instant,
    val origin: HealthDataOrigin = HealthDataOrigin.HEALTH_CONNECT_AGGREGATE
) {
    init {
        require(totalSteps == null || totalSteps >= 0L) {
            "Step total cannot be negative."
        }
        require(!intervalStart.isAfter(intervalEnd)) {
            "Step interval start cannot be after its end."
        }
        require(!receivedAt.isBefore(intervalEnd)) {
            "Receive timestamp cannot be before the end of the queried interval."
        }
        require(origin == HealthDataOrigin.HEALTH_CONNECT_AGGREGATE) {
            "This snapshot type is reserved for Health Connect aggregates."
        }
    }

    val hasRecords: Boolean
        get() = totalSteps != null
}
