package com.luizmarquesarruda.relogio20.data.discovery

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Versioned binary codec for the private on-device discovery library.
 * It contains no Android dependencies so the format can be unit tested.
 */
internal object DiscoveryLibraryCodec {
    const val SCHEMA_VERSION = 1
    const val MAGIC = 0x524C324C
    const val MAX_RECORDS = 1_000
    private const val MAX_FILE_BYTES = 16 * 1024 * 1024

    fun encode(snapshot: DiscoveryLibrarySnapshot): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.writeInt(MAGIC)
            data.writeInt(SCHEMA_VERSION)
            val session = snapshot.activeSession
            data.writeBoolean(session != null)
            if (session != null) {
                data.writeUTF(session.id.take(128))
                data.writeLong(session.startedAtMillis)
                data.writeLong(session.endsAtMillis)
                data.writeInt(session.durationSeconds)
            }

            val records = snapshot.records.takeLast(MAX_RECORDS)
            data.writeInt(records.size)
            records.forEach { record ->
                data.writeUTF(record.id.take(128))
                data.writeLong(record.occurredAtMillis)
                writeNullable(data, record.sessionId)
                data.writeUTF(record.kind.take(64))
                data.writeUTF(record.title.take(256))
                data.writeUTF(record.detail.take(3_000))
                data.writeUTF(record.source.take(128))
                data.writeUTF(record.confidence.take(64))
                writeNullable(data, record.serviceUuid)
                writeNullable(data, record.characteristicUuid)
                writeNullable(data, record.payloadHex?.take(2_048))
            }
        }
        val encoded = output.toByteArray()
        require(encoded.size <= MAX_FILE_BYTES) { "Biblioteca excedeu o limite de armazenamento." }
        return encoded
    }

    fun decode(bytes: ByteArray): DiscoveryLibraryReadResult {
        if (bytes.isEmpty() || bytes.size > MAX_FILE_BYTES) return DiscoveryLibraryReadResult.Corrupt
        return try {
            DataInputStream(ByteArrayInputStream(bytes)).use { data ->
                if (data.readInt() != MAGIC) return DiscoveryLibraryReadResult.Corrupt
                val schema = data.readInt()
                if (schema > SCHEMA_VERSION) {
                    return DiscoveryLibraryReadResult.UnsupportedSchema(schema)
                }
                if (schema != SCHEMA_VERSION) return DiscoveryLibraryReadResult.Corrupt

                val session = if (data.readBoolean()) {
                    DiscoveryTestSession(
                        id = data.readUTF(),
                        startedAtMillis = data.readLong(),
                        endsAtMillis = data.readLong(),
                        durationSeconds = data.readInt()
                    )
                } else {
                    null
                }

                val count = data.readInt()
                if (count !in 0..MAX_RECORDS) return DiscoveryLibraryReadResult.Corrupt
                val records = ArrayList<DiscoveryLibraryRecord>(count)
                repeat(count) {
                    records += DiscoveryLibraryRecord(
                        id = data.readUTF(),
                        occurredAtMillis = data.readLong(),
                        sessionId = readNullable(data),
                        kind = data.readUTF(),
                        title = data.readUTF(),
                        detail = data.readUTF(),
                        source = data.readUTF(),
                        confidence = data.readUTF(),
                        serviceUuid = readNullable(data),
                        characteristicUuid = readNullable(data),
                        payloadHex = readNullable(data)
                    )
                }
                if (data.available() != 0) return DiscoveryLibraryReadResult.Corrupt
                DiscoveryLibraryReadResult.Available(
                    DiscoveryLibrarySnapshot(
                        schemaVersion = schema,
                        activeSession = session,
                        records = records
                    )
                )
            }
        } catch (_: Exception) {
            DiscoveryLibraryReadResult.Corrupt
        }
    }

    private fun writeNullable(data: DataOutputStream, value: String?) {
        data.writeBoolean(value != null)
        if (value != null) data.writeUTF(value.take(6_000))
    }

    private fun readNullable(data: DataInputStream): String? =
        if (data.readBoolean()) data.readUTF() else null
}
