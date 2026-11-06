package com.luizmarquesarruda.relogio20.device.ble.core

/**
 * Pure decision layer for reconnect scheduling.
 *
 * Android timers and BLE operations remain in the Activity for now; this class
 * owns only the retry gates and decisions so they can be tested independently.
 */
internal class BleReconnectCoordinator(
    private val reconnectPolicy: BleReconnectPolicy
) {
    sealed class ScheduleDecision {
        object Disabled : ScheduleDecision()
        object DeferredToForeground : ScheduleDecision()
        object AlreadyActive : ScheduleDecision()
        data class Scheduled(val delayMillis: Long) : ScheduleDecision()
    }

    enum class AttemptDecision {
        Skip,
        PermissionRequired,
        StartScan
    }

    fun planSchedule(
        activityResumed: Boolean,
        gattSessionExists: Boolean,
        scanInProgress: Boolean,
        immediate: Boolean = false
    ): ScheduleDecision {
        if (!reconnectPolicy.isEnabled) return ScheduleDecision.Disabled
        if (!activityResumed) return ScheduleDecision.DeferredToForeground
        if (gattSessionExists || scanInProgress) return ScheduleDecision.AlreadyActive

        val delayMillis = if (immediate) {
            0L
        } else {
            reconnectPolicy.nextRetryDelayMillis() ?: return ScheduleDecision.Disabled
        }
        return ScheduleDecision.Scheduled(delayMillis)
    }

    fun evaluateAttempt(
        activityResumed: Boolean,
        gattSessionExists: Boolean,
        scanInProgress: Boolean,
        hasBlePermissions: () -> Boolean
    ): AttemptDecision {
        if (!activityResumed || !reconnectPolicy.isEnabled ||
            gattSessionExists || scanInProgress
        ) {
            return AttemptDecision.Skip
        }
        return if (hasBlePermissions()) {
            AttemptDecision.StartScan
        } else {
            AttemptDecision.PermissionRequired
        }
    }
}
