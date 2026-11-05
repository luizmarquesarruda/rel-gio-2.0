package com.luizmarquesarruda.relogio20.data.discovery

internal object DiscoveryLibraryKind {
    const val DISCOVERY = "DISCOVERY"
    const val NOTIFICATION = "NOTIFICATION"
    const val GATT_READ = "GATT_READ"
    const val SESSION_STARTED = "SESSION_STARTED"
    const val SESSION_FINISHED = "SESSION_FINISHED"

    val all: Set<String> = setOf(
        DISCOVERY, NOTIFICATION, GATT_READ, SESSION_STARTED, SESSION_FINISHED
    )
}

internal data class DiscoveryTestSession(
    val id: String,
    val startedAtMillis: Long,
    val endsAtMillis: Long,
    val durationSeconds: Int
) {
    init {
        require(id.isNotBlank() && id.length <= 128)
        require(startedAtMillis >= 0L)
        require(endsAtMillis > startedAtMillis)
        require(DiscoveryTestDuration.isAllowed(durationSeconds))
        require(endsAtMillis - startedAtMillis == durationSeconds.toLong() * 1_000L)
    }

    fun isActive(nowMillis: Long): Boolean =
        nowMillis >= startedAtMillis && nowMillis < endsAtMillis

    fun remainingSeconds(nowMillis: Long): Int {
        val effectiveNow = nowMillis.coerceAtLeast(startedAtMillis)
        val remainingMillis = (endsAtMillis - effectiveNow).coerceAtLeast(0L)
        return ((remainingMillis + 999L) / 1_000L).toInt()
    }
}

internal data class DiscoveryLibraryRecord(
    val id: String,
    val occurredAtMillis: Long,
    val sessionId: String?,
    val kind: String,
    val title: String,
    val detail: String,
    val source: String,
    val confidence: String,
    val serviceUuid: String? = null,
    val characteristicUuid: String? = null,
    val payloadHex: String? = null
)

internal data class DiscoveryLibrarySnapshot(
    val schemaVersion: Int = DiscoveryLibraryCodec.SCHEMA_VERSION,
    val activeSession: DiscoveryTestSession? = null,
    val records: List<DiscoveryLibraryRecord> = emptyList()
)

/** Returns a copy without exactly one matching record, or null when no match exists. */
internal fun DiscoveryLibrarySnapshot.withoutRecord(recordId: String): DiscoveryLibrarySnapshot? {
    if (recordId.isBlank()) return null
    val remaining = records.filterNot { it.id == recordId }
    return if (remaining.size == records.size) null else copy(records = remaining)
}

internal sealed class DiscoveryLibraryReadResult {
    object Empty : DiscoveryLibraryReadResult()
    data class Available(val snapshot: DiscoveryLibrarySnapshot) : DiscoveryLibraryReadResult()
    object Corrupt : DiscoveryLibraryReadResult()
    data class UnsupportedSchema(val version: Int) : DiscoveryLibraryReadResult()
}

internal object DiscoveryTestDuration {
    val allowedSeconds: List<Int> = listOf(60, 120, 300, 600)

    fun isAllowed(seconds: Int): Boolean = seconds in allowedSeconds
}
