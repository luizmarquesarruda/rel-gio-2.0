package com.luizmarquesarruda.relogio20.domain.health

import com.luizmarquesarruda.relogio20.domain.telemetry.TelemetrySample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** GATT inventory/discovery does not constitute a measurement stream. */
internal class DiscoveryOnlySource : HealthMetricsSource {
    override suspend fun initiateSession(): Result<SessionHandle> =
        Result.success(SessionHandle.DiscoveryOnly("gatt-discovery-only"))

    override fun observeMetrics(handle: SessionHandle): Flow<TelemetrySample> =
        when (handle) {
            is SessionHandle.DiscoveryOnly -> emptyFlow()
            is SessionHandle.Streaming -> emptyFlow()
        }
}
