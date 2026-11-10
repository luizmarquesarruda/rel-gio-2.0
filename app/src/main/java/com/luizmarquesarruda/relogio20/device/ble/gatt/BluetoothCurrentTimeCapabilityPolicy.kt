package com.luizmarquesarruda.relogio20.device.ble.gatt

/** Safety gate for automatic Bluetooth SIG Current Time writes. */
internal object BluetoothCurrentTimeCapabilityPolicy {
    enum class Capability {
        DISCONNECTED,
        SERVICE_MISSING,
        CHARACTERISTIC_MISSING,
        WRITE_UNSUPPORTED,
        DEVICE_PROFILE_UNCONFIRMED,
        READY,
    }

    fun evaluate(
        connected: Boolean,
        servicePresent: Boolean,
        characteristicPresent: Boolean,
        supportsWrite: Boolean,
        deviceProfileConfirmed: Boolean = false,
    ): Capability =
        when {
            !connected -> Capability.DISCONNECTED
            !servicePresent -> Capability.SERVICE_MISSING
            !characteristicPresent -> Capability.CHARACTERISTIC_MISSING
            !supportsWrite -> Capability.WRITE_UNSUPPORTED
            !deviceProfileConfirmed -> Capability.DEVICE_PROFILE_UNCONFIRMED
            else -> Capability.READY
        }
}
