package com.luizmarquesarruda.relogio20.data.discovery

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.util.UUID

/**
 * App-private, atomic, bounded library for GATT discoveries and test-session readings.
 * Raw HEX is retained only while an explicit timed capture session is active.
 */
internal class DiscoveryLibraryStore(context: Context) {
    private val atomicFile = AtomicFile(
        File(context.applicationContext.filesDir, FILE_NAME)
    )

    @Synchronized
    fun read(): DiscoveryLibraryReadResult {
        if (!atomicFile.baseFile.isFile) return DiscoveryLibraryReadResult.Empty
        return try {
            atomicFile.openRead().use { input ->
                DiscoveryLibraryCodec.decode(input.readBytes())
            }
        } catch (_: Exception) {
            DiscoveryLibraryReadResult.Corrupt
        }
    }

    @Synchronized
    fun startTestSession(durationSeconds: Int, nowMillis: Long): Boolean {
        if (!DiscoveryTestDuration.isAllowed(durationSeconds) || nowMillis < 0L) return false
        val current = writableSnapshot() ?: return false
        val existing = current.activeSession
        if (existing != null && existing.isActive(nowMillis)) return false

        val base = if (existing != null) {
            finishSnapshot(current, nowMillis, "Duração encerrada antes de iniciar nova sessão")
        } else {
            current
        }
        val session = DiscoveryTestSession(
            id = UUID.randomUUID().toString(),
            startedAtMillis = nowMillis,
            endsAtMillis = nowMillis + durationSeconds * 1_000L,
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
        return persist(base.copy(
            activeSession = session,
            records = (base.records + startRecord).takeLast(DiscoveryLibraryCodec.MAX_RECORDS)
        ))
    }

    @Synchronized
    fun finishTestSession(nowMillis: Long, reason: String = "Encerrado pelo usuário"): Boolean {
        val current = writableSnapshot() ?: return false
        if (current.activeSession == null) return false
        return persist(finishSnapshot(current, nowMillis, reason))
    }

    @Synchronized
    fun activeSession(nowMillis: Long): DiscoveryTestSession? {
        val current = (read() as? DiscoveryLibraryReadResult.Available)?.snapshot ?: return null
        val session = current.activeSession ?: return null
        if (session.isActive(nowMillis)) return session
        finishTestSession(nowMillis, "Tempo de captura concluído")
        return null
    }

    @Synchronized
    fun recordsForSession(sessionId: String): Int {
        val snapshot = (read() as? DiscoveryLibraryReadResult.Available)?.snapshot ?: return 0
        return snapshot.records.count { it.sessionId == sessionId &&
            it.kind != DiscoveryLibraryKind.SESSION_STARTED &&
            it.kind != DiscoveryLibraryKind.SESSION_FINISHED
        }
    }

    @Synchronized
    fun recordDiscovery(
        title: String,
        detail: String,
        nowMillis: Long,
        source: String = "INVENTARIO_GATT",
        confidence: String = "OBSERVADO"
    ): Boolean {
        val current = writableSnapshot() ?: return false
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
        return persist(current.copy(
            records = (current.records + record).takeLast(DiscoveryLibraryCodec.MAX_RECORDS)
        ))
    }

    /** Persists a decoded value only inside the active, consented capture session. */
    @Synchronized
    fun recordSessionDiscovery(
        title: String,
        detail: String,
        nowMillis: Long,
        source: String = "GATT_SESSION",
        confidence: String = "OBSERVADO"
    ): Boolean {
        if (nowMillis < 0L) return false
        val current = writableSnapshot() ?: return false
        val session = current.activeSession ?: return false
        if (!session.isActive(nowMillis)) {
            persist(finishSnapshot(current, nowMillis, "Tempo de captura concluído"))
            return false
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
        return persist(current.copy(
            records = (current.records + record).takeLast(DiscoveryLibraryCodec.MAX_RECORDS)
        ))
    }

    @Synchronized
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
    ): Boolean {
        val current = writableSnapshot() ?: return false
        val session = current.activeSession ?: return false
        if (!session.isActive(nowMillis)) {
            finishTestSession(nowMillis, "Tempo de captura concluído")
            return false
        }
        if (kind != DiscoveryLibraryKind.NOTIFICATION && kind != DiscoveryLibraryKind.GATT_READ) {
            return false
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
            serviceUuid = serviceUuid.take(128),
            characteristicUuid = characteristicUuid.take(128),
            payloadHex = payloadHex.take(2_048)
        )
        return persist(current.copy(
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
        val captured = current.records.count { it.sessionId == session.id &&
            it.kind != DiscoveryLibraryKind.SESSION_STARTED &&
            it.kind != DiscoveryLibraryKind.SESSION_FINISHED
        }
        val summary = DiscoveryLibraryRecord(
            id = UUID.randomUUID().toString(),
            occurredAtMillis = nowMillis,
            sessionId = session.id,
            kind = DiscoveryLibraryKind.SESSION_FINISHED,
            title = "Teste BLE finalizado",
            detail = reason + " • " + captured + " registros de leitura/notificação salvos.",
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
            atomicFile.failWrite(stream)
            false
        }
    }

    companion object {
        private const val FILE_NAME = "discovery-library-v1.bin"
    }
}
