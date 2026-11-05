package com.luizmarquesarruda.relogio20.domain.health

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class HealthMetricSampleTest {
    @Test
    fun unvalidatedWatchSampleIsNotPhysicallyValidated() {
        val sample = HealthMetricSample(
            type = HealthMetricType.BLOOD_OXYGEN, value = 98.0,
            unit = HealthMetricUnit.PERCENT, origin = HealthDataOrigin.GT3_MAX,
            receivedAtEpochMillis = 1_798_000_000_000L
        )
        assertFalse(sample.isPhysicallyValidatedWatchSample)
    }

    @Test
    fun physicallyValidatedWatchSamplePassesWatchValidationGate() {
        val sample = HealthMetricSample(
            type = HealthMetricType.METT, value = 1.8,
            unit = HealthMetricUnit.MET, origin = HealthDataOrigin.GT3_MAX,
            receivedAtEpochMillis = 1_798_000_000_000L,
            measuredAtEpochMillis = 1_797_999_999_000L,
            validationStatus = HealthValidationStatus.PHYSICALLY_VALIDATED
        )
        assertTrue(sample.isPhysicallyValidatedWatchSample)
    }

    @Test
    fun healthConnectAggregateIsNeverRelabelledAsWatchOrigin() {
        val sample = HealthMetricSample(
            type = HealthMetricType.STEPS, value = 1200.0,
            unit = HealthMetricUnit.COUNT, origin = HealthDataOrigin.HEALTH_CONNECT_AGGREGATE,
            receivedAtEpochMillis = 1_798_000_000_000L,
            validationStatus = HealthValidationStatus.PHYSICALLY_VALIDATED
        )
        assertFalse(sample.isPhysicallyValidatedWatchSample)
    }

    @Test
    fun rejectsMeasurementTimestampAfterReceiptTimestamp() {
        assertThrows(IllegalArgumentException::class.java) {
            HealthMetricSample(
                type = HealthMetricType.HEART_RATE,
                value = 72.0,
                unit = HealthMetricUnit.BEATS_PER_MINUTE,
                origin = HealthDataOrigin.GT3_MAX,
                receivedAtEpochMillis = 1_798_000_000_000L,
                measuredAtEpochMillis = 1_798_000_000_001L
            )
        }
    }

    @Test
    fun rejectsIncompatibleUnits() {
        assertThrows(IllegalArgumentException::class.java) {
            HealthMetricSample(
                type = HealthMetricType.BLOOD_PRESSURE_SYSTOLIC, value = 120.0,
                unit = HealthMetricUnit.PERCENT, origin = HealthDataOrigin.GT3_MAX,
                receivedAtEpochMillis = 1_798_000_000_000L
            )
        }
    }

    @Test
    fun rejectsOutOfRangeBloodOxygen() {
        assertThrows(IllegalArgumentException::class.java) {
            HealthMetricSample(
                type = HealthMetricType.BLOOD_OXYGEN, value = 101.0,
                unit = HealthMetricUnit.PERCENT, origin = HealthDataOrigin.GT3_MAX,
                receivedAtEpochMillis = 1_798_000_000_000L
            )
        }
    }

    @Test
    fun rejectsFractionalStepCounts() {
        assertThrows(IllegalArgumentException::class.java) {
            HealthMetricSample(
                type = HealthMetricType.STEPS, value = 10.5,
                unit = HealthMetricUnit.COUNT, origin = HealthDataOrigin.PHONE,
                receivedAtEpochMillis = 1_798_000_000_000L
            )
        }
    }
}
