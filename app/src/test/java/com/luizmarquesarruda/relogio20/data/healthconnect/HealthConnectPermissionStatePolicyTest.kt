package com.luizmarquesarruda.relogio20.data.healthconnect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectPermissionStatePolicyTest {
    @Test
    fun firstLaunchWithoutPermissionDoesNotPretendUserDenied() {
        val state =
            HealthConnectPermissionStatePolicy.afterPermissionRefresh(
                granted = false,
                previous = HealthConnectPermissionState.NOT_REQUESTED,
            )

        assertEquals(HealthConnectPermissionState.NOT_REQUESTED, state)
        assertTrue(HealthConnectPermissionStatePolicy.shouldRequestPermission(state))
        assertFalse(HealthConnectPermissionStatePolicy.shouldOfferSettings(state))
    }

    @Test
    fun deniedRequestIsPersistentlyRoutedToSettingsRecovery() {
        val state = HealthConnectPermissionStatePolicy.afterPermissionResult(granted = false)

        assertEquals(HealthConnectPermissionState.DENIED, state)
        assertTrue(HealthConnectPermissionStatePolicy.shouldOfferSettings(state))
        assertFalse(HealthConnectPermissionStatePolicy.shouldRequestPermission(state))
    }

    @Test
    fun grantedRequestClearsPreviousDenialState() {
        val state = HealthConnectPermissionStatePolicy.afterPermissionResult(granted = true)

        assertEquals(HealthConnectPermissionState.GRANTED, state)
        assertFalse(HealthConnectPermissionStatePolicy.shouldOfferSettings(state))
    }

    @Test
    fun permissionRevocationAfterGrantRequiresSettingsRecovery() {
        val state =
            HealthConnectPermissionStatePolicy.afterPermissionRefresh(
                granted = false,
                previous = HealthConnectPermissionState.GRANTED,
            )

        assertEquals(HealthConnectPermissionState.REVOKED, state)
        assertTrue(HealthConnectPermissionStatePolicy.shouldOfferSettings(state))
    }

    @Test
    fun settingsGrantIsDetectedAndClearsRecoveryState() {
        val state =
            HealthConnectPermissionStatePolicy.afterPermissionRefresh(
                granted = true,
                previous = HealthConnectPermissionState.DENIED,
            )

        assertEquals(HealthConnectPermissionState.GRANTED, state)
        assertFalse(HealthConnectPermissionStatePolicy.shouldOfferSettings(state))
    }

    @Test
    fun deniedStateSurvivesRefreshWhilePermissionRemainsMissing() {
        val state =
            HealthConnectPermissionStatePolicy.afterPermissionRefresh(
                granted = false,
                previous = HealthConnectPermissionState.DENIED,
            )

        assertEquals(HealthConnectPermissionState.DENIED, state)
        assertTrue(HealthConnectPermissionStatePolicy.shouldOfferSettings(state))
    }
}
