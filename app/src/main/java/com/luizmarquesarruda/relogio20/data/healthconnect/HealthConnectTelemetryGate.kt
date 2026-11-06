package com.luizmarquesarruda.relogio20.data.healthconnect

import com.luizmarquesarruda.relogio20.domain.telemetry.TelemetrySample
import com.luizmarquesarruda.relogio20.domain.telemetry.TrustLevel

internal fun interface ValidatedTelemetrySink {
    suspend fun write(sample: TelemetrySample)
}

/** Port only: typed Health Connect writes remain disabled until parsers are validated. */
internal class HealthConnectTelemetryGate(private val sink: ValidatedTelemetrySink) {
    suspend fun writeIfEligible(sample: TelemetrySample) {
        require(sample.trustLevel == TrustLevel.VALIDADO) {
            "Health Connect write denied: sample trustLevel must be VALIDADO"
        }
        sink.write(sample.defensiveCopy())
    }
}
