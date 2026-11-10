package com.luizmarquesarruda.relogio20.device.discovery

/**
 * Allows recovery only while the same GATT connection is still active and no
 * services-discovered callback has been received.
 */
internal object GattDiscoveryRecoveryPolicy {
    fun shouldRecover(
        connectionIsCurrent: Boolean,
        connected: Boolean,
        disconnecting: Boolean,
        callbackReceived: Boolean
    ): Boolean = connectionIsCurrent && connected && !disconnecting && !callbackReceived
}
