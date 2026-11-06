package com.luizmarquesarruda.relogio20.device.discovery

/** Bounded retry counter for a successful GATT callback that exposes an empty service list. */
internal class EmptyInventoryRetryPolicy(val maxAttempts: Int) {
    private var attempts = 0

    init { require(maxAttempts >= 1) }

    @Synchronized
    fun nextRetry(): Int? {
        if (attempts >= maxAttempts) return null
        attempts += 1
        return attempts
    }

    @Synchronized
    fun reset() {
        attempts = 0
    }

    @Synchronized
    fun attemptsUsed(): Int = attempts
}
