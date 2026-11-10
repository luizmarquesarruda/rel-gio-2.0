package com.luizmarquesarruda.relogio20.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.luizmarquesarruda.relogio20.domain.health.StepsSnapshot
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

internal enum class HealthConnectAvailability {
    AVAILABLE,
    PROVIDER_UNAVAILABLE,
    PROVIDER_UPDATE_REQUIRED
}

internal sealed class HealthConnectStepsResult {
    data class Success(val snapshot: StepsSnapshot) : HealthConnectStepsResult()
    object PermissionRequired : HealthConnectStepsResult()
    object ProviderUnavailable : HealthConnectStepsResult()
    object ProviderUpdateRequired : HealthConnectStepsResult()
    data class Failure(val reason: String) : HealthConnectStepsResult()
}

/**
 * Android data adapter for the aggregate daily step count.
 *
 * UI code does not access HealthConnectClient, record classes, or query builders.
 * This adapter deliberately reads aggregate steps only; it does not claim that
 * the result came from the GT3 MAX or write any data back to Health Connect.
 */
internal class HealthConnectStepsRepository(context: Context) {
    private val appContext = context.applicationContext

    val readPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    fun availability(): HealthConnectAvailability =
        when (HealthConnectClient.getSdkStatus(appContext)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectAvailability.PROVIDER_UNAVAILABLE
            else -> HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED
        }

    suspend fun hasReadPermission(): Boolean {
        if (availability() != HealthConnectAvailability.AVAILABLE) return false
        val client = HealthConnectClient.getOrCreate(appContext)
        return client.permissionController.getGrantedPermissions().containsAll(readPermissions)
    }

    suspend fun readTodaySteps(
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): HealthConnectStepsResult {
        when (availability()) {
            HealthConnectAvailability.PROVIDER_UNAVAILABLE ->
                return HealthConnectStepsResult.ProviderUnavailable
            HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED ->
                return HealthConnectStepsResult.ProviderUpdateRequired
            HealthConnectAvailability.AVAILABLE -> Unit
        }

        return try {
            val client = HealthConnectClient.getOrCreate(appContext)
            val granted = client.permissionController.getGrantedPermissions()
            if (!granted.containsAll(readPermissions)) {
                return HealthConnectStepsResult.PermissionRequired
            }

            val start = now.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, now)
                )
            )

            HealthConnectStepsResult.Success(
                StepsSnapshot(
                    totalSteps = response[StepsRecord.COUNT_TOTAL],
                    intervalStart = start,
                    intervalEnd = now,
                    receivedAt = Instant.now()
                )
            )
        } catch (error: SecurityException) {
            HealthConnectStepsResult.PermissionRequired
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            HealthConnectStepsResult.Failure(
                error.message ?: "Unknown Health Connect error."
            )
        }
    }
}
