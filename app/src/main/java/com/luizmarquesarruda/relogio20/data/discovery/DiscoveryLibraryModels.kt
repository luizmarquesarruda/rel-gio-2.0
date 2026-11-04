package com.luizmarquesarruda.relogio20.data.discovery

internal object DiscoveryLibraryKind {
    const val DISCOVERY = "DISCOVERY"
    const val NOTIFICATION = "NOTIFICATION"
    const val GATT_READ = "GATT_READ"
    const val SESSION_STARTED = "SESSION_STARTED"
    const val SESSION_FINISHED = "SESSION_FINISHED"
}

internal data class DiscoveryTestSession(
    val id: String,
    val startedAtMillis: Long,
    val endsAtMillis: Long,
    val durationSeconds: Int
) {
    fun isActive(nowMillis: Long): Boolean = nowMillis < endsAtMillis

    fun remainingSeconds(nowMillis: Long): Int {
        val remainingMillis = (endsAtMillis - nowMillis).coerceAtLeast(0L)
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
