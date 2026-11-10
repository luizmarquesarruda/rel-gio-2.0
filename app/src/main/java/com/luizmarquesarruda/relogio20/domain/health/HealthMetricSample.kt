package com.luizmarquesarruda.relogio20.domain.health

/**
 * Normalized health/activity sample. This model deliberately contains no Android or BLE types.
 * Raw protocol bytes must be retained separately until their meaning is established.
 */
enum class HealthMetricType {
    HEART_RATE,
    BLOOD_OXYGEN,
    BLOOD_PRESSURE_SYSTOLIC,
    BLOOD_PRESSURE_DIASTOLIC,
    STEPS,
    SLEEP_DURATION,
    METT,
    STRESS,
}

enum class HealthMetricUnit {
    BEATS_PER_MINUTE,
    PERCENT,
    MMHG,
    COUNT,
    MINUTES,
    MET,
    UNKNOWN,
}

enum class HealthDataOrigin {
    GT3_MAX,
    PHONE,
    HEALTH_CONNECT_AGGREGATE,
    WEARFIT_PRO,
    MANUAL,
}

enum class HealthValidationStatus {
    UNVALIDATED,
    SEMANTICS_VALIDATED,
    PHYSICALLY_VALIDATED,
}

data class HealthMetricSample(
    val type: HealthMetricType,
    val value: Double,
    val unit: HealthMetricUnit,
    val origin: HealthDataOrigin,
    val receivedAtEpochMillis: Long,
    val measuredAtEpochMillis: Long? = null,
    val validationStatus: HealthValidationStatus = HealthValidationStatus.UNVALIDATED,
) {
    init {
        require(value.isFinite()) { "Metric value must be finite." }
        require(value >= 0.0) { "Metric value cannot be negative." }
        require(receivedAtEpochMillis > 0L) { "Receive timestamp must be positive." }
        require(measuredAtEpochMillis == null || measuredAtEpochMillis > 0L) {
            "Measurement timestamp must be positive when present."
        }
        require(measuredAtEpochMillis == null || measuredAtEpochMillis <= receivedAtEpochMillis) {
            "Measurement timestamp cannot be after its receive timestamp."
        }
        require(unit == expectedUnit(type)) { "Unit $unit is incompatible with metric $type." }
        if (type == HealthMetricType.BLOOD_OXYGEN) {
            require(value in 0.0..100.0) { "Blood oxygen percentage must be between 0 and 100." }
        }
        if (type == HealthMetricType.HEART_RATE ||
            type == HealthMetricType.BLOOD_PRESSURE_SYSTOLIC ||
            type == HealthMetricType.BLOOD_PRESSURE_DIASTOLIC ||
            type == HealthMetricType.METT
        ) {
            require(value > 0.0) { "This metric must be greater than zero." }
        }
        if (type == HealthMetricType.STEPS) {
            require(value % 1.0 == 0.0) { "Step count must be a whole number." }
        }
    }

    /** True only after repeatable physical validation of a sample originating at the watch. */
    val isPhysicallyValidatedWatchSample: Boolean
        get() =
            origin == HealthDataOrigin.GT3_MAX &&
                validationStatus == HealthValidationStatus.PHYSICALLY_VALIDATED

    companion object {
        private fun expectedUnit(type: HealthMetricType): HealthMetricUnit =
            when (type) {
                HealthMetricType.HEART_RATE -> HealthMetricUnit.BEATS_PER_MINUTE

                HealthMetricType.BLOOD_OXYGEN -> HealthMetricUnit.PERCENT

                HealthMetricType.BLOOD_PRESSURE_SYSTOLIC,
                HealthMetricType.BLOOD_PRESSURE_DIASTOLIC,
                -> HealthMetricUnit.MMHG

                HealthMetricType.STEPS -> HealthMetricUnit.COUNT

                HealthMetricType.SLEEP_DURATION -> HealthMetricUnit.MINUTES

                HealthMetricType.METT -> HealthMetricUnit.MET

                HealthMetricType.STRESS -> HealthMetricUnit.UNKNOWN
            }
    }
}
