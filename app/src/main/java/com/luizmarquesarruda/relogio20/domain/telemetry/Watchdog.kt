package com.luizmarquesarruda.relogio20.domain.telemetry

internal class TelemetryWatchdog(private val stallAfterMs: Long) {
    init { require(stallAfterMs > 0L) }

    fun tick(state: TelemetryState, nowElapsedMs: Long): TelemetryState {
        val reference = when (state) {
            is TelemetryState.Subscribed -> state.sinceElapsedMs
            is TelemetryState.AwaitingHandshake -> state.sinceElapsedMs
            is TelemetryState.Streaming -> state.lastNotifyElapsedMs
            else -> return state
        }
        return if (nowElapsedMs - reference > stallAfterMs) {
            TelemetryState.Stalled(reference)
        } else state
    }
}
