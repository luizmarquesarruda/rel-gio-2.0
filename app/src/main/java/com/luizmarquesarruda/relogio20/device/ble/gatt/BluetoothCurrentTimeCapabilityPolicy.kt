package com.luizmarquesarruda.relogio20.device.ble.gatt

/** Pure gate for offering a manual Bluetooth SIG Current Time write. */
internal object BluetoothCurrentTimeCapabilityPolicy {
    enum class Capability {
        DISCONNECTED,
        SERVICE_MISSING,
        CHARACTERISTIC_MISSING,
        WRITE_UNSUPPORTED,
        READY
    }

    fun evaluate(
        connected: Boolean,
        servicePresent: Boolean,
        characteristicPresent: Boolean,
        supportsWrite: Boolean
    ): Capability = when {
        !connected -> Capability.DISCONNECTED
        !servicePresent -> Capability.SERVICE_MISSING
        !characteristicPresent -> Capability.CHARACTERISTIC_MISSING
        !supportsWrite -> Capability.WRITE_UNSUPPORTED
        else -> Capability.READY
    }
}
