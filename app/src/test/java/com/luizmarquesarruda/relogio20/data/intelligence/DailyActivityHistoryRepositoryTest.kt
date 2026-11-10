package com.luizmarquesarruda.relogio20.data.intelligence

import com.luizmarquesarruda.relogio20.domain.health.HealthDataOrigin
import com.luizmarquesarruda.relogio20.domain.health.HealthValidationStatus
import com.luizmarquesarruda.relogio20.domain.intelligence.DailyActivityObservation
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class DailyActivityHistoryRepositoryTest {
    private class MemoryStorage(var bytes: ByteArray? = null) : DailyActivityHistoryStorage {
        var failWrites = false

        override fun readBytes(): ByteArray? = bytes?.copyOf()

        override fun writeAtomically(bytes: ByteArray): Boolean {
            if (failWrites) return false
            this.bytes = bytes.copyOf()
            return true
        }
    }

    private fun observation(
        day: Long = 20L,
        steps: Long = 6_000L,
        origin: HealthDataOrigin = HealthDataOrigin.HEALTH_CONNECT_AGGREGATE,
        receivedAt: Long = 1_800_000_000_000L
    ) = DailyActivityObservation(
        day,
        steps,
        origin,
        receivedAt,
        if (origin == HealthDataOrigin.GT3_MAX) {
            HealthValidationStatus.PHYSICALLY_VALIDATED
        } else {
            HealthValidationStatus.UNVALIDATED
        }
    )

    @Test
    fun storesAndReadsWithProvenance() {
        val repository = DailyActivityHistoryRepository(MemoryStorage())
        assertEquals(DailyActivityHistoryWriteResult.SAVED, repository.upsert(observation()))
        val saved = (repository.read() as DailyActivityHistoryReadResult.Available)
            .snapshot.observations.single()
        assertEquals(observation(), saved)
    }

    @Test
    fun duplicateIsIdempotent() {
        val repository = DailyActivityHistoryRepository(MemoryStorage())
        val sample = observation()
        assertEquals(DailyActivityHistoryWriteResult.SAVED, repository.upsert(sample))
        assertEquals(DailyActivityHistoryWriteResult.UNCHANGED, repository.upsert(sample))
    }

    @Test
    fun newerReplacesSameDayAndOrigin() {
        val repository = DailyActivityHistoryRepository(MemoryStorage())
        repository.upsert(observation(steps = 5_000L, receivedAt = 100L))
        assertEquals(
            DailyActivityHistoryWriteResult.SAVED,
            repository.upsert(observation(steps = 7_000L, receivedAt = 200L))
        )
        assertEquals(
            7_000L,
            (repository.read() as DailyActivityHistoryReadResult.Available)
                .snapshot.observations.single().steps
        )
    }

    @Test
    fun olderCannotOverwriteNewer() {
        val repository = DailyActivityHistoryRepository(MemoryStorage())
        repository.upsert(observation(steps = 7_000L, receivedAt = 200L))
        assertEquals(
            DailyActivityHistoryWriteResult.STALE_IGNORED,
            repository.upsert(observation(steps = 2_000L, receivedAt = 100L))
        )
    }

    @Test
    fun differentOriginsRemainSeparate() {
        val repository = DailyActivityHistoryRepository(MemoryStorage())
        repository.upsert(observation())
        assertEquals(
            DailyActivityHistoryWriteResult.SAVED,
            repository.upsert(observation(origin = HealthDataOrigin.PHONE))
        )
        assertEquals(
            2,
            (repository.read() as DailyActivityHistoryReadResult.Available)
                .snapshot.observations.size
        )
    }

    @Test
    fun queryFiltersOriginAndDateRange() {
        val repository = DailyActivityHistoryRepository(MemoryStorage())
        repository.upsert(observation(day = 19L))
        repository.upsert(observation(day = 20L))
        repository.upsert(observation(day = 21L, origin = HealthDataOrigin.PHONE))
        assertEquals(
            listOf(19L, 20L),
            repository.observationsFor(
                HealthDataOrigin.HEALTH_CONNECT_AGGREGATE,
                19L,
                20L
            ).map { it.epochDay }
        )
    }

    @Test
    fun corruptFileIsNeverOverwritten() {
        val original = byteArrayOf(1, 2, 3, 4)
        val storage = MemoryStorage(original)
        val repository = DailyActivityHistoryRepository(storage)
        assertEquals(DailyActivityHistoryWriteResult.CORRUPT, repository.upsert(observation()))
        assertArrayEquals(original, storage.bytes)
    }

    @Test
    fun futureSchemaIsNeverOverwritten() {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use {
            it.writeInt(0x52324148)
            it.writeInt(DailyActivityHistoryCodec.SCHEMA_VERSION + 1)
        }
        val original = output.toByteArray()
        val storage = MemoryStorage(original)
        val repository = DailyActivityHistoryRepository(storage)
        assertEquals(
            DailyActivityHistoryWriteResult.UNSUPPORTED_SCHEMA,
            repository.upsert(observation())
        )
        assertArrayEquals(original, storage.bytes)
    }

    @Test
    fun failedWritePreservesPreviousBytes() {
        val storage = MemoryStorage()
        val repository = DailyActivityHistoryRepository(storage)
        repository.upsert(observation())
        val original = storage.bytes!!.copyOf()
        storage.failWrites = true
        assertEquals(
            DailyActivityHistoryWriteResult.STORAGE_FAILURE,
            repository.upsert(observation(day = 21L))
        )
        assertArrayEquals(original, storage.bytes)
    }

    @Test
    fun codecRejectsDuplicateDayAndOrigin() {
        val sample = observation()
        try {
            DailyActivityHistoryCodec.encode(
                DailyActivityHistorySnapshot(observations = listOf(sample, sample.copy(steps = 9_000L)))
            )
            fail("Expected duplicate key.")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun codecRejectsTrailingBytes() {
        val encoded = DailyActivityHistoryCodec.encode(
            DailyActivityHistorySnapshot(observations = listOf(observation()))
        )
        assertEquals(
            DailyActivityHistoryReadResult.Corrupt::class.java,
            DailyActivityHistoryCodec.decode(encoded + byteArrayOf(0))::class.java
        )
    }

    @Test
    fun decoderRejectsNegativeSteps() {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use {
            it.writeInt(0x52324148)
            it.writeInt(DailyActivityHistoryCodec.SCHEMA_VERSION)
            it.writeInt(1)
            it.writeLong(20L)
            it.writeLong(-1L)
            it.writeUTF(HealthDataOrigin.HEALTH_CONNECT_AGGREGATE.name)
            it.writeLong(1_800_000_000_000L)
            it.writeUTF(HealthValidationStatus.UNVALIDATED.name)
        }
        assertEquals(
            DailyActivityHistoryReadResult.Corrupt::class.java,
            DailyActivityHistoryCodec.decode(output.toByteArray())::class.java
        )
    }
}
