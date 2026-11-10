package com.luizmarquesarruda.relogio20.data.healthconnect

/**
 * Persistable local state for the user's READ_STEPS permission decision.
 * This state never substitutes for checking the provider's current grant.
 */
internal enum class HealthConnectPermissionState {
    NOT_REQUESTED,
    DENIED,
    GRANTED,
    REVOKED,
}

internal object HealthConnectPermissionStatePolicy {
    fun afterPermissionResult(granted: Boolean): HealthConnectPermissionState =
        if (granted) HealthConnectPermissionState.GRANTED else HealthConnectPermissionState.DENIED

    fun afterPermissionRefresh(
        granted: Boolean,
        previous: HealthConnectPermissionState,
    ): HealthConnectPermissionState =
        when {
            granted -> HealthConnectPermissionState.GRANTED
            previous == HealthConnectPermissionState.GRANTED -> HealthConnectPermissionState.REVOKED
            else -> previous
        }

    fun shouldOfferSettings(state: HealthConnectPermissionState): Boolean =
        state == HealthConnectPermissionState.DENIED ||
            state == HealthConnectPermissionState.REVOKED

    fun shouldRequestPermission(state: HealthConnectPermissionState): Boolean = state == HealthConnectPermissionState.NOT_REQUESTED
}
