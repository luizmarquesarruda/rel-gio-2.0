package com.luizmarquesarruda.relogio20.domain.activity

/** Pure policies for features that can run on Android without a watch protocol. */
object LocalFeaturePolicy {
    const val DEFAULT_STEP_GOAL = 8_000
    const val MIN_STEP_GOAL = 100
    const val MAX_STEP_GOAL = 100_000

    fun normalizeStepGoal(value: Int): Int {
        require(value in MIN_STEP_GOAL..MAX_STEP_GOAL) {
            "Step goal must be between $MIN_STEP_GOAL and $MAX_STEP_GOAL."
        }
        return value
    }

    fun progressPercent(currentSteps: Long?, goal: Int): Int {
        normalizeStepGoal(goal)
        if (currentSteps == null || currentSteps < 0L) return 0
        return ((currentSteps * 100L) / goal.toLong()).coerceIn(0L, 100L).toInt()
    }

    fun remainingSteps(currentSteps: Long?, goal: Int): Long? {
        normalizeStepGoal(goal)
        return currentSteps?.takeIf { it >= 0L }?.let { (goal.toLong() - it).coerceAtLeast(0L) }
    }
}
