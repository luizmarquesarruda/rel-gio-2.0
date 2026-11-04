package com.luizmarquesarruda.relogio20.device.ble.core

/**
 * Keeps the app from reclaiming a BLE peripheral while the user has handed it
 * to its original companion app (for example, Wearfit Pro).
 */
internal object BleExternalHandoffPolicy {
    fun canAutoReconnect(handoffActive: Boolean, reconnectEnabled: Boolean): Boolean =
        !handoffActive && reconnectEnabled

    fun shouldStartInitialDiscovery(handoffActive: Boolean, initialDiscoveryPending: Boolean): Boolean =
        !handoffActive && initialDiscoveryPending
}
