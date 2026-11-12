package com.luizmarquesarruda.relogio20.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.luizmarquesarruda.relogio20.domain.health.StepsSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

internal enum class HealthConnectAvailability {
    AVAILABLE,
    PROVIDER_UNAVAILABLE,
    PROVIDER_UPDATE_REQUIRED,
}

internal sealed class HealthConnectStepsResult {
    data class Success(
        val snapshot: StepsSnapshot,
    ) : HealthConnectStepsResult()

    object PermissionRequired : HealthConnectStepsResult()

    object ProviderUnavailable : HealthConnectStepsResult()

    object ProviderUpdateRequired : HealthConnectStepsResult()

    data class Failure(
        val reason: String,
    ) : HealthConnectStepsResult()
}

internal data class StepsHistoryDay(
    val date: LocalDate,
    val totalSteps: Long?,
)

internal sealed class HealthConnectStepsHistoryResult {
    data class Success(
        val days: List<StepsHistoryDay>,
    ) : HealthConnectStepsHistoryResult()

    object PermissionRequired : HealthConnectStepsHistoryResult()

    data class Failure(
        val reason: String,
    ) : HealthConnectStepsHistoryResult()
}

/**
 * Android data adapter for the aggregate daily step count.
 *
 * UI code does not access HealthConnectClient, record classes, or query builders.
 * This adapter deliberately reads aggregate steps only; it does not claim that
 * the result came from the GT3 MAX or write any data back to Health Connect.
 */
internal class HealthConnectStepsRepository(
    context: Context,
) {
    private val appContext = context.applicationContext

    val readPermissions: Set<String> =
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
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

    /**
     * Returns daily Health Connect aggregates for a bounded period. Empty days stay null:
     * no bucket is not silently converted to a zero-step measurement.
     */
    suspend fun readStepsHistory(
        days: Int = 7,
        zoneId: ZoneId = ZoneId.systemDefault(),
        now: java.time.LocalDateTime = java.time.LocalDateTime.now(zoneId),
        originPolicy: HealthConnectOriginPolicy = HealthConnectOriginPolicy.APP_ONLY,
    ): HealthConnectStepsHistoryResult {
        require(days in 1..31) { "History window must be between 1 and 31 days." }
        when (availability()) {
            HealthConnectAvailability.PROVIDER_UNAVAILABLE -> {
                return HealthConnectStepsHistoryResult.Failure("Health Connect não disponível.")
            }

            HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED -> {
                return HealthConnectStepsHistoryResult.Failure("Health Connect precisa ser atualizado.")
            }

            HealthConnectAvailability.AVAILABLE -> {
                Unit
            }
        }
        return try {
            val client = HealthConnectClient.getOrCreate(appContext)
            if (!client.permissionController.getGrantedPermissions().containsAll(readPermissions)) {
                return HealthConnectStepsHistoryResult.PermissionRequired
            }
            val today = now.toLocalDate()
            val firstDay = today.minusDays((days - 1).toLong())
            val buckets =
                client
                    .aggregateGroupByPeriod(
                        AggregateGroupByPeriodRequest(
                            metrics = setOf(StepsRecord.COUNT_TOTAL),
                            timeRangeFilter =
                                TimeRangeFilter.between(
                                    firstDay.atStartOfDay(),
                                    now,
                                ),
                            timeRangeSlicer = Period.ofDays(1),
                            dataOriginFilter = originPolicy.dataOriginFilter(appContext.packageName),
                        ),
                    ).associate { bucket ->
                        bucket.startTime.toLocalDate() to bucket.result[StepsRecord.COUNT_TOTAL]
                    }
            HealthConnectStepsHistoryResult.Success(
                (0 until days).map { offset ->
                    val date = firstDay.plusDays(offset.toLong())
                    StepsHistoryDay(date, buckets[date])
                },
            )
        } catch (error: SecurityException) {
            HealthConnectStepsHistoryResult.PermissionRequired
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            HealthConnectStepsHistoryResult.Failure(
                error.message ?: "Falha ao consultar histórico de passos.",
            )
        }
    }

    suspend fun readTodaySteps(
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        originPolicy: HealthConnectOriginPolicy = HealthConnectOriginPolicy.APP_ONLY,
    ): HealthConnectStepsResult {
        when (availability()) {
            HealthConnectAvailability.PROVIDER_UNAVAILABLE -> {
                return HealthConnectStepsResult.ProviderUnavailable
            }

            HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED -> {
                return HealthConnectStepsResult.ProviderUpdateRequired
            }

            HealthConnectAvailability.AVAILABLE -> {
                Unit
            }
        }

        return try {
            val client = HealthConnectClient.getOrCreate(appContext)
            val granted = client.permissionController.getGrantedPermissions()
            if (!granted.containsAll(readPermissions)) {
                return HealthConnectStepsResult.PermissionRequired
            }

            val start = HealthConnectDayWindow.startOfDay(now, zoneId)
            val response =
                client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(start, now),
                        dataOriginFilter = originPolicy.dataOriginFilter(appContext.packageName),
                    ),
                )

            HealthConnectStepsResult.Success(
                StepsSnapshot(
                    totalSteps = response[StepsRecord.COUNT_TOTAL],
                    intervalStart = start,
                    intervalEnd = now,
                    receivedAt = Instant.now(),
                ),
            )
        } catch (error: SecurityException) {
            HealthConnectStepsResult.PermissionRequired
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            HealthConnectStepsResult.Failure(
                error.message ?: "Unknown Health Connect error.",
            )
        }
    }
}
