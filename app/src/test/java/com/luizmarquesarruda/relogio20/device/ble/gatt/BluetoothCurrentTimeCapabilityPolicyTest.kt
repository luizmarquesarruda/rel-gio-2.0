package com.luizmarquesarruda.relogio20.device.ble.gatt

import org.junit.Assert.assertEquals
import org.junit.Test

class BluetoothCurrentTimeCapabilityPolicyTest {
    @Test
    fun requiresAnActiveConnection() {
        assertEquals(
            BluetoothCurrentTimeCapabilityPolicy.Capability.DISCONNECTED,
            BluetoothCurrentTimeCapabilityPolicy.evaluate(
                connected = false,
                servicePresent = true,
                characteristicPresent = true,
                supportsWrite = true,
            ),
        )
    }

    @Test
    fun rejectsMissingCurrentTimeService() {
        assertEquals(
            BluetoothCurrentTimeCapabilityPolicy.Capability.SERVICE_MISSING,
            BluetoothCurrentTimeCapabilityPolicy.evaluate(
                connected = true,
                servicePresent = false,
                characteristicPresent = false,
                supportsWrite = false,
            ),
        )
    }

    @Test
    fun rejectsMissingCurrentTimeCharacteristic() {
        assertEquals(
            BluetoothCurrentTimeCapabilityPolicy.Capability.CHARACTERISTIC_MISSING,
            BluetoothCurrentTimeCapabilityPolicy.evaluate(
                connected = true,
                servicePresent = true,
                characteristicPresent = false,
                supportsWrite = false,
            ),
        )
    }

    @Test
    fun rejectsCharacteristicWithoutWriteProperty() {
        assertEquals(
            BluetoothCurrentTimeCapabilityPolicy.Capability.WRITE_UNSUPPORTED,
            BluetoothCurrentTimeCapabilityPolicy.evaluate(
                connected = true,
                servicePresent = true,
                characteristicPresent = true,
                supportsWrite = false,
            ),
        )
    }

    @Test
    fun enablesAutomaticSyncOnlyForConfirmedProfileAndEveryGattGate() {
        assertEquals(
            BluetoothCurrentTimeCapabilityPolicy.Capability.READY,
            BluetoothCurrentTimeCapabilityPolicy.evaluate(
                connected = true,
                servicePresent = true,
                characteristicPresent = true,
                supportsWrite = true,
                deviceProfileConfirmed = true,
            ),
        )
    }

    @Test
    fun rejectsCtsWriteForUnconfirmedBleDevice() {
        assertEquals(
            BluetoothCurrentTimeCapabilityPolicy.Capability.DEVICE_PROFILE_UNCONFIRMED,
            BluetoothCurrentTimeCapabilityPolicy.evaluate(
                connected = true,
                servicePresent = true,
                characteristicPresent = true,
                supportsWrite = true,
                deviceProfileConfirmed = false,
            ),
        )
    }
}
