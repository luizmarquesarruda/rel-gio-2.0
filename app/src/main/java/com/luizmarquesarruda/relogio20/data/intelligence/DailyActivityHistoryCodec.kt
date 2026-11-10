package com.luizmarquesarruda.relogio20.data.intelligence

import com.luizmarquesarruda.relogio20.domain.health.HealthDataOrigin
import com.luizmarquesarruda.relogio20.domain.health.HealthValidationStatus
import com.luizmarquesarruda.relogio20.domain.intelligence.DailyActivityObservation
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

internal data class DailyActivityHistorySnapshot(
    val schemaVersion: Int = DailyActivityHistoryCodec.SCHEMA_VERSION,
    val observations: List<DailyActivityObservation> = emptyList()
)

internal sealed class DailyActivityHistoryReadResult {
    object Empty : DailyActivityHistoryReadResult()
    data class Available(val snapshot: DailyActivityHistorySnapshot) : DailyActivityHistoryReadResult()
    object Corrupt : DailyActivityHistoryReadResult()
    object StorageFailure : DailyActivityHistoryReadResult()
    data class UnsupportedSchema(val version: Int) : DailyActivityHistoryReadResult()
}

/** Stable, Android-free binary format for daily activity history. */
internal object DailyActivityHistoryCodec {
    const val SCHEMA_VERSION = 1
    const val MAX_OBSERVATIONS = 4_000
    const val MAX_FILE_BYTES = 1_048_576
    private const val MAGIC = 0x52324148

    fun encode(snapshot: DailyActivityHistorySnapshot): ByteArray {
        require(snapshot.schemaVersion == SCHEMA_VERSION)
        require(snapshot.observations.size <= MAX_OBSERVATIONS)

        val keys = HashSet<Pair<Long, HealthDataOrigin>>()
        snapshot.observations.forEach { observation ->
            require(keys.add(observation.epochDay to observation.origin)) {
                "Duplicate day/source observation."
            }
        }

        val ordered = snapshot.observations.sortedWith(
            compareBy<DailyActivityObservation> { it.epochDay }.thenBy { it.origin.name }
        )
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.writeInt(MAGIC)
            data.writeInt(SCHEMA_VERSION)
            data.writeInt(ordered.size)
            ordered.forEach { item ->
                data.writeLong(item.epochDay)
                data.writeLong(item.steps)
                data.writeUTF(item.origin.name)
                data.writeLong(item.receivedAtEpochMillis)
                data.writeUTF(item.validationStatus.name)
            }
        }

        return output.toByteArray().also {
            require(it.size <= MAX_FILE_BYTES) { "History file size limit reached." }
        }
    }

    fun decode(bytes: ByteArray): DailyActivityHistoryReadResult {
        if (bytes.isEmpty() || bytes.size > MAX_FILE_BYTES) {
            return DailyActivityHistoryReadResult.Corrupt
        }

        return try {
            DataInputStream(ByteArrayInputStream(bytes)).use { data ->
                if (data.readInt() != MAGIC) return DailyActivityHistoryReadResult.Corrupt

                val schema = data.readInt()
                if (schema > SCHEMA_VERSION) {
                    return DailyActivityHistoryReadResult.UnsupportedSchema(schema)
                }
                if (schema != SCHEMA_VERSION) return DailyActivityHistoryReadResult.Corrupt

                val count = data.readInt()
                if (count !in 0..MAX_OBSERVATIONS) return DailyActivityHistoryReadResult.Corrupt

                val observations = ArrayList<DailyActivityObservation>(count)
                val keys = HashSet<Pair<Long, HealthDataOrigin>>()
                repeat(count) {
                    val day = data.readLong()
                    val steps = data.readLong()
                    val origin = HealthDataOrigin.valueOf(data.readUTF())
                    val received = data.readLong()
                    val validation = HealthValidationStatus.valueOf(data.readUTF())
                    val item = DailyActivityObservation(day, steps, origin, received, validation)

                    if (!keys.add(day to origin)) return DailyActivityHistoryReadResult.Corrupt
                    observations += item
                }

                if (data.available() != 0) return DailyActivityHistoryReadResult.Corrupt
                DailyActivityHistoryReadResult.Available(
                    DailyActivityHistorySnapshot(schema, observations)
                )
            }
        } catch (_: Exception) {
            DailyActivityHistoryReadResult.Corrupt
        }
    }
}
