package com.luizmarquesarruda.relogio20.domain.health

import com.luizmarquesarruda.relogio20.domain.telemetry.TelemetrySample
import kotlinx.coroutines.flow.Flow

sealed interface SessionHandle {
    val id: String
    data class DiscoveryOnly(override val id: String) : SessionHandle
    data class Streaming(override val id: String) : SessionHandle
}

interface HealthMetricsSource {
    suspend fun initiateSession(): Result<SessionHandle>
    fun observeMetrics(handle: SessionHandle): Flow<TelemetrySample>
}
