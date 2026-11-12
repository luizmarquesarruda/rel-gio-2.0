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
    const val MAX_FILE_BYTES = 16 * 1024 * 1024
    private const val MAX_PAYLOAD_HEX_CHARS = 2_048
    private val payloadPattern = Regex("(?:[0-9A-Fa-f]{2})(?:-?[0-9A-Fa-f]{2})*")

    fun encode(snapshot: DiscoveryLibrarySnapshot): ByteArray {
        require(snapshot.schemaVersion == SCHEMA_VERSION) { "Schema não suportado para gravação." }
        require(snapshot.records.size <= MAX_RECORDS) { "Quantidade de registros excede o limite." }
        validateRecords(snapshot.records)

        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.writeInt(MAGIC)
            data.writeInt(SCHEMA_VERSION)
            val session = snapshot.activeSession
            data.writeBoolean(session != null)
            if (session != null) {
                data.writeUTF(session.id)
                data.writeLong(session.startedAtMillis)
                data.writeLong(session.endsAtMillis)
                data.writeInt(session.durationSeconds)
            }

            data.writeInt(snapshot.records.size)
            snapshot.records.forEach { record ->
                data.writeUTF(record.id)
                data.writeLong(record.occurredAtMillis)
                writeNullable(data, record.sessionId)
                data.writeUTF(record.kind)
                data.writeUTF(record.title)
                data.writeUTF(record.detail)
                data.writeUTF(record.source)
                data.writeUTF(record.confidence)
                writeNullable(data, record.serviceUuid)
                writeNullable(data, record.characteristicUuid)
                writeNullable(data, record.payloadHex)
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

                val session =
                    if (data.readBoolean()) {
                        DiscoveryTestSession(
                            id = data.readUTF(),
                            startedAtMillis = data.readLong(),
                            endsAtMillis = data.readLong(),
                            durationSeconds = data.readInt(),
                        )
                    } else {
                        null
                    }

                val count = data.readInt()
                if (count !in 0..MAX_RECORDS) return DiscoveryLibraryReadResult.Corrupt
                val records = ArrayList<DiscoveryLibraryRecord>(count)
                repeat(count) {
                    records +=
                        DiscoveryLibraryRecord(
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
                            payloadHex = readNullable(data),
                        )
                }
                if (data.available() != 0) return DiscoveryLibraryReadResult.Corrupt
                validateRecords(records)
                DiscoveryLibraryReadResult.Available(
                    DiscoveryLibrarySnapshot(
                        schemaVersion = schema,
                        activeSession = session,
                        records = records,
                    ),
                )
            }
        } catch (_: Exception) {
            DiscoveryLibraryReadResult.Corrupt
        }
    }

    private fun validateRecords(records: List<DiscoveryLibraryRecord>) {
        require(records.map { it.id }.toSet().size == records.size) {
            "A biblioteca contém identificadores de registro duplicados."
        }
        records.forEach { record ->
            require(record.id.isNotBlank() && record.id.length <= 128)
            require(record.occurredAtMillis >= 0L)
            require(record.sessionId == null || (record.sessionId.isNotBlank() && record.sessionId.length <= 128))
            require(record.kind in DiscoveryLibraryKind.all)
            require(record.title.length <= 256)
            require(record.detail.length <= 6_000)
            require(record.source.length <= 128)
            require(record.confidence.length <= 64)
            require(record.serviceUuid == null || isBoundedIdentifier(record.serviceUuid))
            require(record.characteristicUuid == null || isBoundedIdentifier(record.characteristicUuid))
            require(record.payloadHex == null || isValidPayloadHex(record.payloadHex))
            if (record.kind == DiscoveryLibraryKind.SESSION_STARTED ||
                record.kind == DiscoveryLibraryKind.SESSION_FINISHED ||
                record.kind == DiscoveryLibraryKind.NOTIFICATION ||
                record.kind == DiscoveryLibraryKind.GATT_READ
            ) {
                require(record.sessionId != null)
            }
            if (record.payloadHex != null) {
                require(record.serviceUuid != null && record.characteristicUuid != null)
            }
        }
    }

    private fun isBoundedIdentifier(value: String): Boolean = value.isNotBlank() && value.length <= 128 && value.none { it.isWhitespace() }

    private fun isValidPayloadHex(value: String): Boolean =
        value == "(vazio)" || (value.length <= MAX_PAYLOAD_HEX_CHARS && payloadPattern.matches(value))

    private fun writeNullable(
        data: DataOutputStream,
        value: String?,
    ) {
        data.writeBoolean(value != null)
        if (value != null) data.writeUTF(value)
    }

    private fun readNullable(data: DataInputStream): String? = if (data.readBoolean()) data.readUTF() else null
}
