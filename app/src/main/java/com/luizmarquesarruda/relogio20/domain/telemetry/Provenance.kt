package com.luizmarquesarruda.relogio20.domain.telemetry

data class Provenance(
    val source: String,
    val deviceFingerprint: String? = null,
    val sourceTimestamp: Long? = null,
) {
    init {
        require(source.isNotBlank())
        require(sourceTimestamp == null || sourceTimestamp >= 0L)
    }
}
