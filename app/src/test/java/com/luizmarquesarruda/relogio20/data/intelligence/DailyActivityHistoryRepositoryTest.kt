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
  override fun writeAtomically(bytes: ByteArray): Boolean { if (failWrites) return false; this.bytes = bytes.copyOf(); return true }
 }
 private fun item(day: Long = 20L, steps: Long = 6000L, origin: HealthDataOrigin = HealthDataOrigin.HEALTH_CONNECT_AGGREGATE, received: Long = 1_800_000_000_000L) =
  DailyActivityObservation(day, steps, origin, received, if (origin == HealthDataOrigin.GT3_MAX) HealthValidationStatus.PHYSICALLY_VALIDATED else HealthValidationStatus.UNVALIDATED)
 @Test fun storesAndReadsWithProvenance() {
  val repo = DailyActivityHistoryRepository(MemoryStorage())
  assertEquals(DailyActivityHistoryWriteResult.SAVED, repo.upsert(item()))
  assertEquals(item(), (repo.read() as DailyActivityHistoryReadResult.Available).snapshot.observations.single())
 }
 @Test fun duplicateIsIdempotent() {
  val repo = DailyActivityHistoryRepository(MemoryStorage()); val sample = item()
  assertEquals(DailyActivityHistoryWriteResult.SAVED, repo.upsert(sample))
  assertEquals(DailyActivityHistoryWriteResult.UNCHANGED, repo.upsert(sample))
 }
 @Test fun newerReplacesSameDayAndOrigin() {
  val repo = DailyActivityHistoryRepository(MemoryStorage())
  repo.upsert(item(steps = 5000L, received = 100L))
  assertEquals(DailyActivityHistoryWriteResult.SAVED, repo.upsert(item(steps = 7000L, received = 200L)))
  assertEquals(7000L, (repo.read() as DailyActivityHistoryReadResult.Available).snapshot.observations.single().steps)
 }
 @Test fun olderCannotOverwriteNewer() {
  val repo = DailyActivityHistoryRepository(MemoryStorage()); repo.upsert(item(steps = 7000L, received = 200L))
  assertEquals(DailyActivityHistoryWriteResult.STALE_IGNORED, repo.upsert(item(steps = 2000L, received = 100L)))
 }
 @Test fun differentOriginsRemainSeparate() {
  val repo = DailyActivityHistoryRepository(MemoryStorage()); repo.upsert(item())
  assertEquals(DailyActivityHistoryWriteResult.SAVED, repo.upsert(item(origin = HealthDataOrigin.PHONE)))
  assertEquals(2, (repo.read() as DailyActivityHistoryReadResult.Available).snapshot.observations.size)
 }
 @Test fun queryFiltersOriginAndDateRange() {
  val repo = DailyActivityHistoryRepository(MemoryStorage())
  repo.upsert(item(day = 19L)); repo.upsert(item(day = 20L)); repo.upsert(item(day = 21L, origin = HealthDataOrigin.PHONE))
  assertEquals(listOf(19L, 20L), repo.observationsFor(HealthDataOrigin.HEALTH_CONNECT_AGGREGATE, 19L, 20L).map { it.epochDay })
 }
 @Test fun corruptFileIsNeverOverwritten() {
  val original = byteArrayOf(1, 2, 3, 4); val storage = MemoryStorage(original); val repo = DailyActivityHistoryRepository(storage)
  assertEquals(DailyActivityHistoryWriteResult.CORRUPT, repo.upsert(item())); assertArrayEquals(original, storage.bytes)
 }
 @Test fun futureSchemaIsNeverOverwritten() {
  val output = ByteArrayOutputStream()
  DataOutputStream(output).use { it.writeInt(0x52324148); it.writeInt(DailyActivityHistoryCodec.SCHEMA_VERSION + 1) }
  val original = output.toByteArray(); val storage = MemoryStorage(original); val repo = DailyActivityHistoryRepository(storage)
  assertEquals(DailyActivityHistoryWriteResult.UNSUPPORTED_SCHEMA, repo.upsert(item())); assertArrayEquals(original, storage.bytes)
 }
 @Test fun failedWritePreservesPreviousBytes() {
  val storage = MemoryStorage(); val repo = DailyActivityHistoryRepository(storage); repo.upsert(item())
  val original = storage.bytes!!.copyOf(); storage.failWrites = true
  assertEquals(DailyActivityHistoryWriteResult.STORAGE_FAILURE, repo.upsert(item(day = 21L)))
  assertArrayEquals(original, storage.bytes)
 }
 @Test fun codecRejectsDuplicateDayAndOrigin() {
  val sample = item()
  try { DailyActivityHistoryCodec.encode(DailyActivityHistorySnapshot(observations = listOf(sample, sample.copy(steps = 9000L)))); fail("Expected duplicate key.") }
  catch (_: IllegalArgumentException) { }
 }
 @Test fun codecRejectsTrailingBytes() {
  val encoded = DailyActivityHistoryCodec.encode(DailyActivityHistorySnapshot(observations = listOf(item())))
  assertEquals(DailyActivityHistoryReadResult.Corrupt::class.java, DailyActivityHistoryCodec.decode(encoded + byteArrayOf(0))::class.java)
 }
 @Test fun decoderRejectsNegativeSteps() {
  val output = ByteArrayOutputStream()
  DataOutputStream(output).use {
   it.writeInt(0x52324148); it.writeInt(DailyActivityHistoryCodec.SCHEMA_VERSION); it.writeInt(1)
   it.writeLong(20L); it.writeLong(-1L); it.writeUTF(HealthDataOrigin.HEALTH_CONNECT_AGGREGATE.name)
   it.writeLong(1_800_000_000_000L); it.writeUTF(HealthValidationStatus.UNVALIDATED.name)
  }
  assertEquals(DailyActivityHistoryReadResult.Corrupt::class.java, DailyActivityHistoryCodec.decode(output.toByteArray())::class.java)
 }
}
