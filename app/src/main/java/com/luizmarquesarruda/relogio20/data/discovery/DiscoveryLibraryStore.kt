package com.luizmarquesarruda.relogio20.data.discovery

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

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
        if (cacheInitialized) {
            return@synchronized cachedReadResult ?: DiscoveryLibraryReadResult.Empty
        }
        val result = readFromDisk()
        cachedReadResult = result
        cacheInitialized = true
        result
    }

    private fun readFromDisk(): DiscoveryLibraryReadResult {
        // AtomicFile may have a valid .bak after a process crash between startWrite()
        // and finishWrite(). Check both candidates, then let openRead() restore the backup.
        val backupFile = File(atomicFile.baseFile.path + ".bak")
        val preferredFile = if (backupFile.isFile) backupFile else atomicFile.baseFile
        val storedLength = preferredFile.takeIf { it.isFile }?.length()
            ?: return DiscoveryLibraryReadResult.Empty
        if (storedLength > DiscoveryLibraryCodec.MAX_FILE_BYTES) {
            return DiscoveryLibraryReadResult.Corrupt
        }
        return try {
            atomicFile.openRead().use { input ->
                DiscoveryLibraryCodec.decode(input.readBytes())
            }
        } catch (_: FileNotFoundException) {
            DiscoveryLibraryReadResult.Empty
        } catch (_: Exception) {
            DiscoveryLibraryReadResult.Corrupt
        }
    }

    /** Queue BLE telemetry writes off the main callback thread, in a single ordered writer. */
    fun recordDiscoveryAsync(
        title: String,
        detail: String,
        nowMillis: Long,
        source: String = "INVENTARIO_GATT",
        confidence: String = "OBSERVADO"
    ): Boolean = enqueueWrite {
        recordDiscovery(title, detail, nowMillis, source, confidence)
    }

    fun recordSessionDiscoveryAsync(
        title: String,
        detail: String,
        nowMillis: Long,
        source: String = "GATT_SESSION",
        confidence: String = "OBSERVADO"
    ): Boolean = enqueueWrite {
        recordSessionDiscovery(title, detail, nowMillis, source, confidence)
    }

    fun recordPayloadAsync(
        kind: String,
        title: String,
        detail: String,
        source: String,
        confidence: String,
        serviceUuid: String,
        characteristicUuid: String,
        payloadHex: String,
        nowMillis: Long
    ): Boolean = enqueueWrite {
        recordPayload(
            kind, title, detail, source, confidence,
            serviceUuid, characteristicUuid, payloadHex, nowMillis
        )
    }

    internal fun flushPendingWrites(timeoutMillis: Long = WRITE_FLUSH_TIMEOUT_MS): Boolean =
        synchronized(WRITE_QUEUE_LOCK) {
            try {
                // The lock makes this barrier a stable boundary: no later enqueue can
                // overtake it or have its failure mixed into the batch being flushed.
                writeExecutor.submit(Runnable { }).get(timeoutMillis, TimeUnit.MILLISECONDS)
                !writeFailureTracker.consumeFailure()
            } catch (_: Exception) {
                false
            }
        }

    private fun enqueueWrite(operation: () -> Boolean): Boolean =
        synchronized(WRITE_QUEUE_LOCK) {
            try {
                writeExecutor.execute {
                    val succeeded = runCatching(operation).getOrDefault(false)
                    writeFailureTracker.record(succeeded)
                }
                true
            } catch (_: RejectedExecutionException) {
                writeFailureTracker.record(false)
                false
            }
        }

    fun startTestSession(durationSeconds: Int, nowMillis: Long): Boolean {
        if (!flushPendingWrites()) return false
        return synchronized(STORE_LOCK) {
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
    }

    fun finishTestSession(nowMillis: Long, reason: String = "Encerrado pelo usuário"): Boolean {
        if (nowMillis < 0L || !flushPendingWrites()) return false
        return synchronized(STORE_LOCK) {
            val current = writableSnapshot() ?: return@synchronized false
            val session = current.activeSession ?: return@synchronized false
            if (nowMillis < session.startedAtMillis) return@synchronized false
            persist(finishSnapshot(current, nowMillis, reason))
        }
    }

    fun activeSession(nowMillis: Long): DiscoveryTestSession? {
        if (nowMillis < 0L) return null
        val session = synchronized(STORE_LOCK) {
            (read() as? DiscoveryLibraryReadResult.Available)?.snapshot?.activeSession
        } ?: return null
        if (nowMillis >= session.endsAtMillis) {
            finishExpiredSessionAsync(session, nowMillis)
            return null
        }
        return session
    }

    private fun finishExpiredSessionAsync(session: DiscoveryTestSession, nowMillis: Long) {
        if (!synchronized(STORE_LOCK) { expiringSessionIds.add(session.id) }) return
        val accepted = enqueueWrite {
            synchronized(STORE_LOCK) {
                try {
                    val current = writableSnapshot() ?: return@synchronized false
                    if (current.activeSession?.id != session.id || nowMillis < session.startedAtMillis) {
                        return@synchronized false
                    }
                    persist(finishSnapshot(current, nowMillis, "Tempo de captura concluído"))
                } finally {
                    expiringSessionIds.remove(session.id)
                }
            }
        }
        if (!accepted) synchronized(STORE_LOCK) { expiringSessionIds.remove(session.id) }
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
            persist(finishSnapshot(current, nowMillis, "Tempo de captura concluído"))
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
            cachedReadResult = DiscoveryLibraryReadResult.Available(snapshot)
            cacheInitialized = true
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
        private const val WRITE_QUEUE_CAPACITY = 2_048
        private const val WRITE_FLUSH_TIMEOUT_MS = 10_000L
        private val PAYLOAD_HEX_PATTERN = Regex("(?:[0-9A-Fa-f]{2})(?:-?[0-9A-Fa-f]{2})*")
        private val STORE_LOCK = Any()
        private val WRITE_QUEUE_LOCK = Any()
        private val writeFailureTracker = WriteFailureTracker()
        private var cacheInitialized = false
        private var cachedReadResult: DiscoveryLibraryReadResult? = null
        private val expiringSessionIds = mutableSetOf<String>()
        private val writeExecutor = ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(WRITE_QUEUE_CAPACITY),
            { runnable -> Thread(runnable, "relogio2-discovery-store").apply { isDaemon = true } },
            ThreadPoolExecutor.AbortPolicy()
        )
    }
}
