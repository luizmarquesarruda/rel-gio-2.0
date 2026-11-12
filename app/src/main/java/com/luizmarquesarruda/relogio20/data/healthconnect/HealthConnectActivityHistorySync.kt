package com.luizmarquesarruda.relogio20.data.healthconnect

import com.luizmarquesarruda.relogio20.data.intelligence.DailyActivityHistoryRepository
import com.luizmarquesarruda.relogio20.data.intelligence.DailyActivityHistoryWriteResult
import com.luizmarquesarruda.relogio20.domain.health.HealthValidationStatus
import com.luizmarquesarruda.relogio20.domain.intelligence.DailyActivityObservation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal enum class HealthConnectActivityHistorySyncStatus {
    SAVED, UNCHANGED, STALE_IGNORED, NO_DATA, INCOMPLETE_DAY, PERMISSION_REQUIRED,
    PROVIDER_UNAVAILABLE, PROVIDER_UPDATE_REQUIRED, CORRUPT_HISTORY,
    UNSUPPORTED_SCHEMA, CAPACITY_REACHED, STORAGE_FAILURE, READ_FAILURE
}

/** Imports only the previous, fully elapsed local calendar day and preserves source provenance. */
internal class HealthConnectActivityHistorySync(
    private val source: CompletedDailyStepsSource,
    private val history: DailyActivityHistoryRepository
) {
    suspend fun syncPreviousCompletedDay(
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): HealthConnectActivityHistorySyncStatus {
        val previousDay = now.atZone(zoneId).toLocalDate().minusDays(1)
        return syncDay(previousDay, now, zoneId)
    }

    internal suspend fun syncDay(
        day: LocalDate,
        now: Instant,
        zoneId: ZoneId
    ): HealthConnectActivityHistorySyncStatus {
        return when (val result = source.readCompletedDaySteps(day, now, zoneId)) {
            is HealthConnectStepsResult.Success -> {
                val steps = result.snapshot.totalSteps
                    ?: return HealthConnectActivityHistorySyncStatus.NO_DATA
                val observation = DailyActivityObservation(
                    epochDay = day.toEpochDay(),
                    steps = steps,
                    origin = result.snapshot.origin,
                    receivedAtEpochMillis = result.snapshot.receivedAt.toEpochMilli(),
                    validationStatus = HealthValidationStatus.UNVALIDATED
                )
                when (history.upsert(observation)) {
                    DailyActivityHistoryWriteResult.SAVED -> HealthConnectActivityHistorySyncStatus.SAVED
                    DailyActivityHistoryWriteResult.UNCHANGED -> HealthConnectActivityHistorySyncStatus.UNCHANGED
                    DailyActivityHistoryWriteResult.STALE_IGNORED -> HealthConnectActivityHistorySyncStatus.STALE_IGNORED
                    DailyActivityHistoryWriteResult.STORAGE_FAILURE -> HealthConnectActivityHistorySyncStatus.STORAGE_FAILURE
                    DailyActivityHistoryWriteResult.CAPACITY_REACHED -> HealthConnectActivityHistorySyncStatus.CAPACITY_REACHED
                    DailyActivityHistoryWriteResult.CORRUPT -> HealthConnectActivityHistorySyncStatus.CORRUPT_HISTORY
                    DailyActivityHistoryWriteResult.UNSUPPORTED_SCHEMA -> HealthConnectActivityHistorySyncStatus.UNSUPPORTED_SCHEMA
                }
            }
            HealthConnectStepsResult.IncompleteDay -> HealthConnectActivityHistorySyncStatus.INCOMPLETE_DAY
            HealthConnectStepsResult.PermissionRequired -> HealthConnectActivityHistorySyncStatus.PERMISSION_REQUIRED
            HealthConnectStepsResult.ProviderUnavailable -> HealthConnectActivityHistorySyncStatus.PROVIDER_UNAVAILABLE
            HealthConnectStepsResult.ProviderUpdateRequired -> HealthConnectActivityHistorySyncStatus.PROVIDER_UPDATE_REQUIRED
            is HealthConnectStepsResult.Failure -> HealthConnectActivityHistorySyncStatus.READ_FAILURE
        }
    }
}
