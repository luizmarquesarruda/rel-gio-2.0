package com.luizmarquesarruda.relogio20.device.ble.core

/**
 * Bounded retry schedule for restoring a previously successful BLE connection.
 * This class is intentionally platform-independent and does not perform BLE operations.
 */
internal class BleReconnectPolicy(initiallyEnabled: Boolean = false) {
    private val retryDelaysMillis = longArrayOf(
        1_000L,
        2_000L,
        5_000L,
        10_000L,
        20_000L,
        30_000L
    )

    var isEnabled: Boolean = initiallyEnabled
        private set

    private var retryIndex = 0

    /** Called only after the Android GATT callback confirms STATE_CONNECTED. */
    fun markConnected() {
        isEnabled = true
        retryIndex = 0
    }

    /** Explicit user intent always takes precedence over pending automatic retries. */
    fun markManuallyDisconnected() {
        isEnabled = false
        retryIndex = 0
    }

    /** Returns the delay before the next retry, or null when recovery is disabled. */
    fun nextRetryDelayMillis(): Long? {
        if (!isEnabled) return null
        val delay = retryDelaysMillis[retryIndex.coerceAtMost(retryDelaysMillis.lastIndex)]
        if (retryIndex < retryDelaysMillis.lastIndex) retryIndex++
        return delay
    }
}