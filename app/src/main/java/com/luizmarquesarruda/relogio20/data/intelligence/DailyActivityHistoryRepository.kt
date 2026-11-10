package com.luizmarquesarruda.relogio20.data.intelligence
import com.luizmarquesarruda.relogio20.domain.health.HealthDataOrigin
import com.luizmarquesarruda.relogio20.domain.intelligence.DailyActivityObservation
/** Storage boundary; implementations must replace the snapshot atomically. */
internal interface DailyActivityHistoryStorage { fun readBytes(): ByteArray?; fun writeAtomically(bytes: ByteArray): Boolean }
internal enum class DailyActivityHistoryWriteResult { SAVED, UNCHANGED, STALE_IGNORED, CAPACITY_REACHED, CORRUPT, UNSUPPORTED_SCHEMA, STORAGE_FAILURE }
/** A day/source pair is unique; older observations never replace newer ones. */
internal class DailyActivityHistoryRepository(private val storage: DailyActivityHistoryStorage) {
 @Synchronized fun read(): DailyActivityHistoryReadResult {
  val bytes = try { storage.readBytes() } catch (_: Exception) { return DailyActivityHistoryReadResult.Corrupt }
   ?: return DailyActivityHistoryReadResult.Empty
  return DailyActivityHistoryCodec.decode(bytes)
 }
 @Synchronized fun upsert(observation: DailyActivityObservation): DailyActivityHistoryWriteResult {
  val current = when (val result = read()) {
   DailyActivityHistoryReadResult.Empty -> DailyActivityHistorySnapshot()
   is DailyActivityHistoryReadResult.Available -> result.snapshot
   DailyActivityHistoryReadResult.Corrupt -> return DailyActivityHistoryWriteResult.CORRUPT
   is DailyActivityHistoryReadResult.UnsupportedSchema -> return DailyActivityHistoryWriteResult.UNSUPPORTED_SCHEMA
  }
  val index = current.observations.indexOfFirst { it.epochDay == observation.epochDay && it.origin == observation.origin }
  val next = current.observations.toMutableList()
  if (index >= 0) {
   val existing = next[index]
   if (observation.receivedAtEpochMillis < existing.receivedAtEpochMillis) return DailyActivityHistoryWriteResult.STALE_IGNORED
   if (observation.receivedAtEpochMillis == existing.receivedAtEpochMillis) {
    return if (observation == existing) DailyActivityHistoryWriteResult.UNCHANGED else DailyActivityHistoryWriteResult.STALE_IGNORED
   }
   next[index] = observation
  } else {
   if (next.size >= DailyActivityHistoryCodec.MAX_OBSERVATIONS) return DailyActivityHistoryWriteResult.CAPACITY_REACHED
   next += observation
  }
  val encoded = try { DailyActivityHistoryCodec.encode(DailyActivityHistorySnapshot(observations = next)) }
   catch (_: IllegalArgumentException) { return DailyActivityHistoryWriteResult.CAPACITY_REACHED }
   catch (_: Exception) { return DailyActivityHistoryWriteResult.STORAGE_FAILURE }
  return if (runCatching { storage.writeAtomically(encoded) }.getOrDefault(false)) DailyActivityHistoryWriteResult.SAVED
   else DailyActivityHistoryWriteResult.STORAGE_FAILURE
 }
 @Synchronized fun observationsFor(origin: HealthDataOrigin, firstEpochDay: Long, lastEpochDay: Long): List<DailyActivityObservation> {
  require(firstEpochDay <= lastEpochDay)
  val snapshot = (read() as? DailyActivityHistoryReadResult.Available)?.snapshot ?: return emptyList()
  return snapshot.observations.filter { it.origin == origin && it.epochDay in firstEpochDay..lastEpochDay }.sortedBy { it.epochDay }
 }
}
