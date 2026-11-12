package com.luizmarquesarruda.relogio20.device.ble.core

import kotlin.random.Random

internal data class ReconnectPolicy(
    val maxAttempts: Int,
    val initialDelayMs: Long,
    val jitter: Double,
) {
    init {
        require(maxAttempts >= 1)
        require(initialDelayMs >= 0L)
        require(jitter in 0.0..1.0)
    }

    fun delayForAttempt(
        attempt: Int,
        random: Random = Random.Default,
    ): Long {
        require(attempt >= 1)
        val multiplier = 1L shl (attempt - 1).coerceAtMost(20)
        val base = initialDelayMs * multiplier
        val spread = (base * jitter).toLong()
        return (base - spread + if (spread == 0L) 0L else random.nextLong(spread * 2L + 1L))
            .coerceAtLeast(0L)
    }

    companion object {
        fun fromBuildConfig(isTest: Boolean): ReconnectPolicy =
            if (isTest) {
                ReconnectPolicy(1, 0L, 0.0)
            } else {
                ReconnectPolicy(3, 1_000L, 0.2)
            }
    }
}
