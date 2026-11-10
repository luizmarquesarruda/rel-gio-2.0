package com.luizmarquesarruda.relogio20.data.healthconnect

/**
 * Bounded retry policy for importing the previous completed Health Connect day.
 * A transient provider/storage failure must not suppress the rest of the local day.
 */
internal object HealthConnectHistorySyncRetryPolicy {
    const val RETRY_INTERVAL_MILLIS = 60L * 60L * 1_000L

    fun shouldAttempt(
        syncKey: String,
        terminalSyncKey: String?,
        lastAttemptElapsedRealtimeMillis: Long?,
        nowElapsedRealtimeMillis: Long
    ): Boolean {
        if (syncKey.isBlank() || syncKey == terminalSyncKey || nowElapsedRealtimeMillis < 0L) {
            return false
        }
        val lastAttempt = lastAttemptElapsedRealtimeMillis ?: return true
        if (lastAttempt < 0L || nowElapsedRealtimeMillis < lastAttempt) return true
        return nowElapsedRealtimeMillis - lastAttempt >= RETRY_INTERVAL_MILLIS
    }

    fun isTerminal(status: HealthConnectActivityHistorySyncStatus): Boolean =
        when (status) {
            HealthConnectActivityHistorySyncStatus.SAVED,
            HealthConnectActivityHistorySyncStatus.UNCHANGED,
            HealthConnectActivityHistorySyncStatus.STALE_IGNORED,
            HealthConnectActivityHistorySyncStatus.CORRUPT_HISTORY,
            HealthConnectActivityHistorySyncStatus.UNSUPPORTED_SCHEMA,
            HealthConnectActivityHistorySyncStatus.CAPACITY_REACHED -> true

            HealthConnectActivityHistorySyncStatus.NO_DATA,
            HealthConnectActivityHistorySyncStatus.INCOMPLETE_DAY,
            HealthConnectActivityHistorySyncStatus.PERMISSION_REQUIRED,
            HealthConnectActivityHistorySyncStatus.PROVIDER_UNAVAILABLE,
            HealthConnectActivityHistorySyncStatus.PROVIDER_UPDATE_REQUIRED,
            HealthConnectActivityHistorySyncStatus.STORAGE_FAILURE,
            HealthConnectActivityHistorySyncStatus.READ_FAILURE -> false
        }
}
