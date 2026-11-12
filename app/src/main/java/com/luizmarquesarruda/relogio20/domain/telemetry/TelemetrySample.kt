package com.luizmarquesarruda.relogio20.domain.telemetry

data class TelemetrySample(
    val raw: ByteArray,
    val receivedAt: Long,
    val trustLevel: TrustLevel,
    val parserVersion: String,
    val provenance: Provenance,
) {
    init {
        require(receivedAt >= 0L)
        require(parserVersion.isNotBlank())
    }

    fun defensiveCopy(): TelemetrySample = copy(raw = raw.copyOf())
}
