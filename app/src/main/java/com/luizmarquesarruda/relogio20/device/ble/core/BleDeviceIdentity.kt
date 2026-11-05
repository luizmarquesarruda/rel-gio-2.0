package com.luizmarquesarruda.relogio20.device.ble.core

internal object BleDeviceIdentity {
    private val pattern = Regex("(?i)^[0-9a-f]{2}(:[0-9a-f]{2}){5}$")
    fun normalize(address: String?): String? {
        val value = address?.trim()?.uppercase() ?: return null
        return value.takeIf { pattern.matches(it) }
    }
    fun matches(expected: String?, observed: String?): Boolean {
        val first = normalize(expected) ?: return false
        val second = normalize(observed) ?: return false
        return first == second
    }
}
