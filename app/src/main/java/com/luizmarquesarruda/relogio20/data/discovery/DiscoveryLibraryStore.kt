package com.luizmarquesarruda.relogio20.data.discovery

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID

/**
 * App-private, atomic, bounded library for GATT discoveries and test-session readings.
 * Raw HEX is retained only while an explicit timed capture session is active.
 * A process-wide lock protects read-modify-write transactions across Activity instances.
 */
internal class DiscoveryLibraryStore(context: Context) {
    private val atomicFile = AtomicFile(
        File(context.applicationContext.filesDir, FILE_NAME)
    )

    fun read(): DiscoveryLibraryReadResult = synchronized(STORE_LOCK) {
        // AtomicFile may have a valid .bak after a process crash between startWrite()
        // and finishWrite(). Check both candidates, then let openRead() restore the backup.
        val backupFile = File(atomicFile.baseFile.path + ".bak")
        val preferredFile = if (backupFile.isFile) backupFile else atomicFile.baseFile
        val storedLength = preferredFile.takeIf { it.isFile }?.length()
            ?: return@synchronized DiscoveryLibraryReadResult.Empty
        if (storedLength > DiscoveryLibraryCodec.MAX_FILE_BYTES) {
            return@synchronized DiscoveryLibraryReadResult.Corrupt
        }
        try {
            atomicFile.openRead().use { input ->
                DiscoveryLibraryCodec.decode(input.readBytes())
            }
        } catch (_: FileNotFoundException) {
            DiscoveryLibraryReadResult.Empty
        } catch (_: Exception) {
            DiscoveryLibraryReadResult.Corrupt
        }
    }

    fun startTestSession(durationSeconds: Int, nowMillis: Long): Boolean =
        synchronized(STORE_LOCK) {
            if (!DiscoveryTestDuration.isAllowed(durationSeconds) || nowMillis < 0L) {
                return@synchronized false
            }
            val endsAtMillis = runCatching {
                Math.addExact(nowMillis, durationSeconds.toLong() * 1_000L)
            }.getOrNull() ?: return@synchronized false
            val current = writableSnapshot() ?: return@synchronized false
            val existing = current.activeSession
            if (existing != null && nowMillis < existing.startedAtMillis) return@synchronized false
            if (existing != null && existing.isActive(nowMillis)) return@synchronized false

            val base = if (existing != null) {
                finishSnapshot(current, nowMillis, "Duração encerrada antes de iniciar nova sessão")
            } else {
                current
            }
            val session = DiscoveryTestSession(
                id = UUID.randomUUID().toString(),
                startedAtMillis = nowMillis,
                endsAtMillis = endsAtMillis,
                durationSeconds = durationSeconds
            )
            val startRecord = DiscoveryLibraryRecord(
                id = UUID.randomUUID().toString(),
                occurredAtMillis = nowMillis,
                sessionId = session.id,
                kind = DiscoveryLibraryKind.SESSION_STARTED,
                title = "Teste BLE iniciado",
                detail = "Captura local por " + durationSeconds / 60 + " min. O aplicativo não envia comandos proprietários.",
                source = "SESSAO_LOCAL",
                confidence = "OBSERVADO"
            )
            persist(base.copy(
                activeSession = session,
                records = (base.records + startRecord).takeLast(DiscoveryLibraryCodec.MAX_RECORDS)
            ))
        }

    fun finishTestSession(nowMillis: Long, reason: String = "Encerrado pelo usuário"): Boolean =
        synchronized(STORE_LOCK) {
            if (nowMillis < 0L) return@synchronized false
            val current = writableSnapshot() ?: return@synchronized false
            val session = current.activeSession ?: return@synchronized false
            if (nowMillis < session.startedAtMillis) return@synchronized false
            persist(finishSnapshot(current, nowMillis, reason))
        }

    fun activeSession(nowMillis: Long): DiscoveryTestSession? = synchronized(STORE_LOCK) {
        if (nowMillis < 0L) return@synchronized null
        val current = (read() as? DiscoveryLibraryReadResult.Available)?.snapshot ?: return@synchronized null
        val session = current.activeSession ?: return@synchronized null
        if (nowMillis < session.startedAtMillis || session.isActive(nowMillis)) return@synchronized session
        finishTestSession(nowMillis, "Tempo de captura concluído")
        null
    }

    fun recordsForSession(sessionId: String): Int = synchronized(STORE_LOCK) {
        val snapshot = (read() as? DiscoveryLibraryReadResult.Available)?.snapshot ?: return@synchronized 0
        snapshot.records.count {
            it.sessionId == sessionId &&
                it.kind != DiscoveryLibraryKind.SESSION_STARTED &&
                it.kind != DiscoveryLibraryKind.SESSION_FINISHED
        }
    }

    fun recordDiscovery(
        title: String,
        detail: String,
        nowMillis: Long,
        source: String = "INVENTARIO_GATT",
        confidence: String = "OBSERVADO"
    ): Boolean = synchronized(STORE_LOCK) {
        if (nowMillis < 0L) return@synchronized false
        val current = writableSnapshot() ?: return@synchronized false
        val record = DiscoveryLibraryRecord(
            id = UUID.randomUUID().toString(),
            occurredAtMillis = nowMillis,
            sessionId = null,
            kind = DiscoveryLibraryKind.DISCOVERY,
            title = title.take(256),
            detail = detail.take(6_000),
            source = source.take(128),
            confidence = confidence.take(64)
        )
        persist(current.copy(
            records = (current.records + record).takeLast(DiscoveryLibraryCodec.MAX_RECORDS)
        ))
    }

    /** Persists a decoded value only inside the active, consented capture session. */
    fun recordSessionDiscovery(
        title: String,
        detail: String,
        nowMillis: Long,
        source: String = "GATT_SESSION",
        confidence: String = "OBSERVADO"
    ): Boolean = synchronized(STORE_LOCK) {
        if (nowMillis < 0L) return@synchronized false
        val current = writableSnapshot() ?: return@synchronized false
        val session = current.activeSession ?: return@synchronized false
        if (nowMillis < session.startedAtMillis) return@synchronized false
        if (!session.isActive(nowMillis)) {
            persist(finishSnapshot(current, nowMillis, "Tempo de captura concluído"))
            return@synchronized false
        }

        val record = DiscoveryLibraryRecord(
            id = UUID.randomUUID().toString(),
            occurredAtMillis = nowMillis,
            sessionId = session.id,
            kind = DiscoveryLibraryKind.DISCOVERY,
            title = title.take(256),
            detail = detail.take(6_000),
            source = source.take(128),
            confidence = confidence.take(64)
        )
        persist(current.copy(
            records = (current.records + record).takeLast(DiscoveryLibraryCodec.MAX_RECORDS)
        ))
    }

    fun recordPayload(
        kind: String,
        title: String,
        detail: String,
        source: String,
        confidence: String,
        serviceUuid: String,
        characteristicUuid: String,
        payloadHex: String,
        nowMillis: Long
    ): Boolean = synchronized(STORE_LOCK) {
        if (nowMillis < 0L ||
            kind != DiscoveryLibraryKind.NOTIFICATION && kind != DiscoveryLibraryKind.GATT_READ ||
            !isBoundedIdentifier(serviceUuid) ||
            !isBoundedIdentifier(characteristicUuid) ||
            !isValidPayloadHex(payloadHex)
        ) return@synchronized false
        val current = writableSnapshot() ?: return@synchronized false
        val session = current.activeSession ?: return@synchronized false
        if (nowMillis < session.startedAtMillis) return@synchronized false
        if (!session.isActive(nowMillis)) {
            finishTestSession(nowMillis, "Tempo de captura concluído")
            return@synchronized false
        }
        val record = DiscoveryLibraryRecord(
            id = UUID.randomUUID().toString(),
            occurredAtMillis = nowMillis,
            sessionId = session.id,
            kind = kind,
            title = title.take(256),
            detail = detail.take(6_000),
            source = source.take(128),
            confidence = confidence.take(64),
            serviceUuid = serviceUuid,
            characteristicUuid = characteristicUuid,
            payloadHex = payloadHex
        )
        persist(current.copy(
            records = (current.records + record).takeLast(DiscoveryLibraryCodec.MAX_RECORDS)
        ))
    }

    private fun writableSnapshot(): DiscoveryLibrarySnapshot? = when (val result = read()) {
        DiscoveryLibraryReadResult.Empty -> DiscoveryLibrarySnapshot()
        is DiscoveryLibraryReadResult.Available -> result.snapshot
        DiscoveryLibraryReadResult.Corrupt,
        is DiscoveryLibraryReadResult.UnsupportedSchema -> null
    }

    private fun finishSnapshot(
        current: DiscoveryLibrarySnapshot,
        nowMillis: Long,
        reason: String
    ): DiscoveryLibrarySnapshot {
        val session = current.activeSession ?: return current
        require(nowMillis >= session.startedAtMillis)
        val captured = current.records.count {
            it.sessionId == session.id &&
                it.kind != DiscoveryLibraryKind.SESSION_STARTED &&
                it.kind != DiscoveryLibraryKind.SESSION_FINISHED
        }
        val summary = DiscoveryLibraryRecord(
            id = UUID.randomUUID().toString(),
            occurredAtMillis = nowMillis,
            sessionId = session.id,
            kind = DiscoveryLibraryKind.SESSION_FINISHED,
            title = "Teste BLE finalizado",
            detail = reason.take(256) + " • " + captured + " registros de leitura/notificação salvos.",
            source = "SESSAO_LOCAL",
            confidence = "OBSERVADO"
        )
        return current.copy(
            activeSession = null,
            records = (current.records + summary).takeLast(DiscoveryLibraryCodec.MAX_RECORDS)
        )
    }

    private fun persist(snapshot: DiscoveryLibrarySnapshot): Boolean {
        val stream = try {
            atomicFile.startWrite()
        } catch (_: Exception) {
            return false
        }
        return try {
            stream.write(DiscoveryLibraryCodec.encode(snapshot))
            atomicFile.finishWrite(stream)
            true
        } catch (_: Exception) {
            runCatching { atomicFile.failWrite(stream) }
            false
        }
    }

    private fun isBoundedIdentifier(value: String): Boolean =
        value.isNotBlank() && value.length <= 128 && value.none { it.isWhitespace() }

    private fun isValidPayloadHex(value: String): Boolean =
        value == "(vazio)" ||
            (value.length <= MAX_PAYLOAD_HEX_CHARS && PAYLOAD_HEX_PATTERN.matches(value))

    companion object {
        private const val FILE_NAME = "discovery-library-v1.bin"
        private const val MAX_PAYLOAD_HEX_CHARS = 2_048
        private val PAYLOAD_HEX_PATTERN = Regex("(?:[0-9A-Fa-f]{2})(?:-?[0-9A-Fa-f]{2})*")
        private val STORE_LOCK = Any()
    }
}
