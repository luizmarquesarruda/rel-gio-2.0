package com.luizmarquesarruda.relogio20.device.ble.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BleReconnectCoordinatorTest {
    @Test
    fun doesNotScheduleWhenRecoveryIsDisabled() {
        val coordinator = BleReconnectCoordinator(BleReconnectPolicy())

        assertEquals(
            BleReconnectCoordinator.ScheduleDecision.Disabled,
            coordinator.planSchedule(
                activityResumed = true,
                gattSessionExists = false,
                scanInProgress = false
            )
        )
    }

    @Test
    fun defersUntilForegroundWithoutConsumingRetryDelay() {
        val coordinator = BleReconnectCoordinator(BleReconnectPolicy(initiallyEnabled = true))

        assertEquals(
            BleReconnectCoordinator.ScheduleDecision.DeferredToForeground,
            coordinator.planSchedule(
                activityResumed = false,
                gattSessionExists = false,
                scanInProgress = false
            )
        )
        assertEquals(
            BleReconnectCoordinator.ScheduleDecision.Scheduled(1_000L),
            coordinator.planSchedule(
                activityResumed = true,
                gattSessionExists = false,
                scanInProgress = false
            )
        )
    }

    @Test
    fun doesNotScheduleWhileGattSessionOrScanIsActive() {
        val coordinator = BleReconnectCoordinator(BleReconnectPolicy(initiallyEnabled = true))

        assertEquals(
            BleReconnectCoordinator.ScheduleDecision.AlreadyActive,
            coordinator.planSchedule(
                activityResumed = true,
                gattSessionExists = true,
                scanInProgress = false
            )
        )
        assertEquals(
            BleReconnectCoordinator.ScheduleDecision.AlreadyActive,
            coordinator.planSchedule(
                activityResumed = true,
                gattSessionExists = false,
                scanInProgress = true
            )
        )
        assertEquals(
            BleReconnectCoordinator.ScheduleDecision.Scheduled(1_000L),
            coordinator.planSchedule(
                activityResumed = true,
                gattSessionExists = false,
                scanInProgress = false
            )
        )
    }

    @Test
    fun immediateScheduleDoesNotConsumeBackoffSequence() {
        val coordinator = BleReconnectCoordinator(BleReconnectPolicy(initiallyEnabled = true))

        assertEquals(
            BleReconnectCoordinator.ScheduleDecision.Scheduled(0L),
            coordinator.planSchedule(
                activityResumed = true,
                gattSessionExists = false,
                scanInProgress = false,
                immediate = true
            )
        )
        assertEquals(
            BleReconnectCoordinator.ScheduleDecision.Scheduled(1_000L),
            coordinator.planSchedule(
                activityResumed = true,
                gattSessionExists = false,
                scanInProgress = false
            )
        )
    }

    @Test
    fun skipsAttemptWithoutCheckingPermissionsWhenStateIsBlocked() {
        val coordinator = BleReconnectCoordinator(BleReconnectPolicy(initiallyEnabled = true))
        var permissionCheckCalled = false

        val decision = coordinator.evaluateAttempt(
            activityResumed = false,
            gattSessionExists = false,
            scanInProgress = false,
            hasBlePermissions = {
                permissionCheckCalled = true
                true
            }
        )

        assertEquals(BleReconnectCoordinator.AttemptDecision.Skip, decision)
        assertFalse(permissionCheckCalled)
    }

    @Test
    fun reportsMissingPermissionOnlyAfterOtherGatesPass() {
        val coordinator = BleReconnectCoordinator(BleReconnectPolicy(initiallyEnabled = true))

        assertEquals(
            BleReconnectCoordinator.AttemptDecision.PermissionRequired,
            coordinator.evaluateAttempt(
                activityResumed = true,
                gattSessionExists = false,
                scanInProgress = false,
                hasBlePermissions = { false }
            )
        )
    }

    @Test
    fun startsScanOnlyWhenAllAttemptGatesPass() {
        val coordinator = BleReconnectCoordinator(BleReconnectPolicy(initiallyEnabled = true))

        assertEquals(
            BleReconnectCoordinator.AttemptDecision.StartScan,
            coordinator.evaluateAttempt(
                activityResumed = true,
                gattSessionExists = false,
                scanInProgress = false,
                hasBlePermissions = { true }
            )
        )
    }
}
