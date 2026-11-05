package com.luizmarquesarruda.relogio20.device.ble.core

import com.luizmarquesarruda.relogio20.device.gt3max.protocol.Gt3MaxCallDeviceProfile

/** BLE discovery hints only; a new identity still requires the captured GATT fingerprint. */
internal object BleScanCandidatePolicy {
    enum class Match { SAVED_ADDRESS, DEVICE_NAME, ADVERTISED_NUS, OTHER }

    fun evaluate(
        savedAddress: String?,
        observedAddress: String?,
        advertisedName: String?,
        advertisedServiceUuids: Set<String>
    ): Match {
        if (BleDeviceIdentity.matches(savedAddress, observedAddress)) return Match.SAVED_ADDRESS
        if (Gt3MaxCallDeviceProfile.isKnownDeviceName(advertisedName)) return Match.DEVICE_NAME
        val hasNus = advertisedServiceUuids.any {
            it.equals(Gt3MaxCallDeviceProfile.NUS_SERVICE_UUID.toString(), ignoreCase = true)
        }
        return if (hasNus) Match.ADVERTISED_NUS else Match.OTHER
    }
}
