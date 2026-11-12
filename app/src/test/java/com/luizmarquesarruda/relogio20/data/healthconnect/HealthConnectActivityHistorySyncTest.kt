package com.luizmarquesarruda.relogio20.data.healthconnect

import com.luizmarquesarruda.relogio20.data.intelligence.DailyActivityHistoryRepository
import com.luizmarquesarruda.relogio20.data.intelligence.DailyActivityHistoryStorage
import com.luizmarquesarruda.relogio20.data.intelligence.DailyActivityHistoryReadResult
import com.luizmarquesarruda.relogio20.domain.health.HealthDataOrigin
import com.luizmarquesarruda.relogio20.domain.health.HealthValidationStatus
import com.luizmarquesarruda.relogio20.domain.intelligence.DailyActivityObservation
import com.luizmarquesarruda.relogio20.domain.health.StepsSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectActivityHistorySyncTest {
    private class MemoryStorage : DailyActivityHistoryStorage {
        var bytes: ByteArray? = null
        override fun readBytes(): ByteArray? = bytes?.copyOf()
        override fun writeAtomically(bytes: ByteArray): Boolean {
            this.bytes = bytes.copyOf()
            return true
        }
    }

    private class FakeSource : CompletedDailyStepsSource {
        var result: HealthConnectStepsResult = HealthConnectStepsResult.IncompleteDay
        var requestedDay: LocalDate? = null
        override suspend fun readCompletedDaySteps(day: LocalDate, now: Instant, zoneId: ZoneId) =
            result.also { requestedDay = day }
    }

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val now = Instant.parse("2026-11-10T15:00:00Z")
    private val previousDay = LocalDate.of(2026, 11, 9)

    private fun snapshot(steps: Long?) = StepsSnapshot(
        totalSteps = steps,
        intervalStart = previousDay.atStartOfDay(zone).toInstant(),
        intervalEnd = previousDay.plusDays(1).atStartOfDay(zone).toInstant(),
        receivedAt = now,
        origin = HealthDataOrigin.HEALTH_CONNECT_AGGREGATE
    )

    @Test
    fun syncTargetsPreviousCalendarDayInSuppliedZone() {
        val source = FakeSource().apply { result = HealthConnectStepsResult.Success(snapshot(5_432L)) }
        val sync = HealthConnectActivityHistorySync(source, DailyActivityHistoryRepository(MemoryStorage()))
        assertEquals(HealthConnectActivityHistorySyncStatus.SAVED, runBlocking {
            sync.syncPreviousCompletedDay(now, zone)
        })
        assertEquals(previousDay, source.requestedDay)
    }

    @Test
    fun missingAggregateDoesNotBecomeZeroOrCreateHistory() {
        val source = FakeSource().apply { result = HealthConnectStepsResult.Success(snapshot(null)) }
        val storage = MemoryStorage()
        val sync = HealthConnectActivityHistorySync(source, DailyActivityHistoryRepository(storage))
        assertEquals(HealthConnectActivityHistorySyncStatus.NO_DATA, runBlocking {
            sync.syncPreviousCompletedDay(now, zone)
        })
        assertEquals(null, storage.bytes)
    }

    @Test
    fun incompleteDayIsNotPersisted() {
        val source = FakeSource()
        val storage = MemoryStorage()
        val sync = HealthConnectActivityHistorySync(source, DailyActivityHistoryRepository(storage))
        assertEquals(HealthConnectActivityHistorySyncStatus.INCOMPLETE_DAY, runBlocking {
            sync.syncDay(LocalDate.of(2026, 11, 10), now, zone)
        })
        assertEquals(null, storage.bytes)
    }

    @Test
    fun repeatedSameAggregateIsIdempotent() {
        val source = FakeSource().apply { result = HealthConnectStepsResult.Success(snapshot(5_432L)) }
        val storage = MemoryStorage()
        val history = DailyActivityHistoryRepository(storage)
        val sync = HealthConnectActivityHistorySync(source, history)
        assertEquals(HealthConnectActivityHistorySyncStatus.SAVED, runBlocking {
            sync.syncPreviousCompletedDay(now, zone)
        })
        val original = storage.bytes!!.copyOf()
        assertEquals(HealthConnectActivityHistorySyncStatus.UNCHANGED, runBlocking {
            sync.syncPreviousCompletedDay(now.plusSeconds(60), zone)
        })
        assertTrue(original.contentEquals(storage.bytes))
        val item = (history.read() as DailyActivityHistoryReadResult.Available).snapshot.observations.single()
        assertEquals(5_432L, item.steps)
        assertEquals(HealthDataOrigin.HEALTH_CONNECT_AGGREGATE, item.origin)
    }

    @Test
    fun olderObservationIsReportedAsStaleAndNewerStoredValueIsPreserved() {
        val source = FakeSource().apply {
            result = HealthConnectStepsResult.Success(snapshot(5_000L))
        }
        val storage = MemoryStorage()
        val history = DailyActivityHistoryRepository(storage)
        val existing = DailyActivityObservation(
            epochDay = previousDay.toEpochDay(),
            steps = 7_000L,
            origin = HealthDataOrigin.HEALTH_CONNECT_AGGREGATE,
            receivedAtEpochMillis = now.plusSeconds(120).toEpochMilli(),
            validationStatus = HealthValidationStatus.UNVALIDATED
        )
        assertEquals(
            com.luizmarquesarruda.relogio20.data.intelligence.DailyActivityHistoryWriteResult.SAVED,
            history.upsert(existing)
        )
        val sync = HealthConnectActivityHistorySync(source, history)

        assertEquals(
            HealthConnectActivityHistorySyncStatus.STALE_IGNORED,
            runBlocking { sync.syncPreviousCompletedDay(now, zone) }
        )
        val saved = (history.read() as DailyActivityHistoryReadResult.Available)
            .snapshot.observations.single()
        assertEquals(7_000L, saved.steps)
    }

    @Test
    fun providerPermissionAndReadFailuresAreNotPersisted() {
        val source = FakeSource()
        val storage = MemoryStorage()
        val sync = HealthConnectActivityHistorySync(source, DailyActivityHistoryRepository(storage))
        val cases = listOf(
            HealthConnectStepsResult.PermissionRequired to HealthConnectActivityHistorySyncStatus.PERMISSION_REQUIRED,
            HealthConnectStepsResult.ProviderUnavailable to HealthConnectActivityHistorySyncStatus.PROVIDER_UNAVAILABLE,
            HealthConnectStepsResult.ProviderUpdateRequired to HealthConnectActivityHistorySyncStatus.PROVIDER_UPDATE_REQUIRED,
            HealthConnectStepsResult.Failure("test") to HealthConnectActivityHistorySyncStatus.READ_FAILURE
        )
        cases.forEach { (input, expected) ->
            source.result = input
            assertEquals(expected, runBlocking { sync.syncPreviousCompletedDay(now, zone) })
        }
        assertEquals(null, storage.bytes)
    }
}
