package com.luizmarquesarruda.relogio20.device.ble.core

/**
 * Pure gates for GATT callbacks. A callback is only allowed to mutate state
 * when it belongs to the current connection and the exact active operation.
 */
internal object GattCallbackSafetyPolicy {
    fun acceptsOperationCallback(
        connectionIsCurrent: Boolean,
        activeOperationKey: String?,
        callbackOperationKey: String,
        invalidatedOperationKeys: Set<String> = emptySet()
    ): Boolean =
        connectionIsCurrent &&
            callbackOperationKey.isNotBlank() &&
            callbackOperationKey !in invalidatedOperationKeys &&
            activeOperationKey == callbackOperationKey

    fun descriptorWriteOperationKey(
        activeOperationKey: String?,
        enableOperationKey: String,
        disableOperationKey: String
    ): String? = when (activeOperationKey) {
        enableOperationKey -> enableOperationKey
        disableOperationKey -> disableOperationKey
        else -> null
    }

    fun acceptsNotificationCallback(
        connectionIsCurrent: Boolean,
        connected: Boolean,
        disconnecting: Boolean
    ): Boolean =
        connectionIsCurrent && connected && !disconnecting

    fun shouldRediscoverServices(
        connectionIsCurrent: Boolean,
        connected: Boolean,
        disconnecting: Boolean,
        handoffActive: Boolean
    ): Boolean =
        connectionIsCurrent && connected && !disconnecting && !handoffActive
}
