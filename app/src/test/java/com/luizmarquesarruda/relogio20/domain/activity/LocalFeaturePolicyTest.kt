package com.luizmarquesarruda.relogio20.domain.activity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class LocalFeaturePolicyTest {
    @Test
    fun progressPreservesMissingAsZeroProgressWithoutClaimingZeroSteps() {
        assertEquals(0, LocalFeaturePolicy.progressPercent(null, 8_000))
        assertNull(LocalFeaturePolicy.remainingSteps(null, 8_000))
    }

    @Test
    fun progressIsBoundedAndRemainingStepsNeverNegative() {
        assertEquals(50, LocalFeaturePolicy.progressPercent(4_000L, 8_000))
        assertEquals(100, LocalFeaturePolicy.progressPercent(12_000L, 8_000))
        assertEquals(0L, LocalFeaturePolicy.remainingSteps(12_000L, 8_000))
    }

    @Test
    fun hugeStepCountCannotOverflowProgress() {
        assertEquals(100, LocalFeaturePolicy.progressPercent(Long.MAX_VALUE, 8_000))
        assertEquals(0L, LocalFeaturePolicy.remainingSteps(Long.MAX_VALUE, 8_000))
    }

    @Test
    fun invalidGoalIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalFeaturePolicy.normalizeStepGoal(0)
        }
    }
}
