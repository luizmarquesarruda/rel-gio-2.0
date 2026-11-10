package com.luizmarquesarruda.relogio20.data.healthconnect

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectHistorySyncRetryPolicyTest {
    @Test
    fun allowsFirstAttemptForANewDay() {
        assertTrue(
            HealthConnectHistorySyncRetryPolicy.shouldAttempt(
                syncKey = "20670@America/Sao_Paulo",
                terminalSyncKey = null,
                lastAttemptSyncKey = null,
                lastAttemptElapsedRealtimeMillis = null,
                nowElapsedRealtimeMillis = 100L
            )
        )
    }

    @Test
    fun delaysRetryAfterTransientFailure() {
        assertFalse(
            HealthConnectHistorySyncRetryPolicy.shouldAttempt(
                syncKey = "20670@America/Sao_Paulo",
                terminalSyncKey = null,
                lastAttemptSyncKey = "20670@America/Sao_Paulo",
                lastAttemptElapsedRealtimeMillis = 100L,
                nowElapsedRealtimeMillis = 100L + HealthConnectHistorySyncRetryPolicy.RETRY_INTERVAL_MILLIS - 1L
            )
        )
        assertTrue(
            HealthConnectHistorySyncRetryPolicy.shouldAttempt(
                syncKey = "20670@America/Sao_Paulo",
                terminalSyncKey = null,
                lastAttemptSyncKey = "20670@America/Sao_Paulo",
                lastAttemptElapsedRealtimeMillis = 100L,
                nowElapsedRealtimeMillis = 100L + HealthConnectHistorySyncRetryPolicy.RETRY_INTERVAL_MILLIS
            )
        )
    }

    @Test
    fun terminalDayIsNotRetriedAndNewDayCanStart() {
        assertFalse(
            HealthConnectHistorySyncRetryPolicy.shouldAttempt(
                syncKey = "20670@America/Sao_Paulo",
                terminalSyncKey = "20670@America/Sao_Paulo",
                lastAttemptSyncKey = "20670@America/Sao_Paulo",
                lastAttemptElapsedRealtimeMillis = 100L,
                nowElapsedRealtimeMillis = 10_000L
            )
        )
        assertTrue(
            HealthConnectHistorySyncRetryPolicy.shouldAttempt(
                syncKey = "20671@America/Sao_Paulo",
                terminalSyncKey = "20670@America/Sao_Paulo",
                lastAttemptSyncKey = "20670@America/Sao_Paulo",
                lastAttemptElapsedRealtimeMillis = 100L,
                nowElapsedRealtimeMillis = 10_000L
            )
        )
    }

    @Test
    fun distinguishesTerminalAndRetryableResults() {
        assertTrue(
            HealthConnectHistorySyncRetryPolicy.isTerminal(
                HealthConnectActivityHistorySyncStatus.STALE_IGNORED
            )
        )
        assertFalse(
            HealthConnectHistorySyncRetryPolicy.isTerminal(
                HealthConnectActivityHistorySyncStatus.STORAGE_FAILURE
            )
        )
        assertFalse(
            HealthConnectHistorySyncRetryPolicy.isTerminal(
                HealthConnectActivityHistorySyncStatus.NO_DATA
            )
        )
    }
}
