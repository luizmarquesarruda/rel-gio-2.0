package com.luizmarquesarruda.relogio20.domain.telemetry

sealed interface TelemetryState {
    data object Idle : TelemetryState

    data class Subscribed(
        val sinceElapsedMs: Long,
    ) : TelemetryState

    data class AwaitingHandshake(
        val sinceElapsedMs: Long,
    ) : TelemetryState

    data class Streaming(
        val lastNotifyElapsedMs: Long,
    ) : TelemetryState

    data class Stalled(
        val sinceElapsedMs: Long,
    ) : TelemetryState
}
