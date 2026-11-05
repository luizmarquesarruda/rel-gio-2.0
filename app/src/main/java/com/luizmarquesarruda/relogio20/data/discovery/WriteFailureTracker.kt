package com.luizmarquesarruda.relogio20.data.discovery

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Retains asynchronous persistence failures until the caller reaches a queue barrier.
 * A successful flush consumes the failure state so later batches are reported independently.
 */
internal class WriteFailureTracker {
    private val failed = AtomicBoolean(false)

    fun record(result: Boolean) {
        if (!result) failed.set(true)
    }

    fun consumeFailure(): Boolean = failed.getAndSet(false)
}
