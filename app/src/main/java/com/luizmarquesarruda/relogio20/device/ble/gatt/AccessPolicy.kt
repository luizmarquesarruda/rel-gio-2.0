package com.luizmarquesarruda.relogio20.device.ble.gatt

import java.util.Collections
import java.util.UUID

/**
 * Fail-closed GATT policy. Characteristic writes are prohibited.
 * The standard CCCD descriptor (0x2902) is handled separately for subscriptions only.
 */
internal data class AccessPolicy(
    val allowRead: Set<UUID> = emptySet(),
    val allowWrite: Set<UUID> = emptySet(),
    val forbidden: Set<UUID> = emptySet(),
) {
    init {
        require(allowWrite.isEmpty()) {
            "Characteristic writes are prohibited by the read-only device policy."
        }
    }

    fun canWrite(
        serviceUuid: UUID,
        characteristicUuid: UUID,
        explicitUserConsent: Boolean,
    ): Boolean = false

    companion object {
        val FE59_DFU_SERVICE = UUID.fromString("0000fe59-0000-1000-8000-00805f9b34fb")
        val DFU_CONTROL_POINT = UUID.fromString("8ec90001-f315-4f60-9fb8-838830daea50")
        val DFU_PACKET = UUID.fromString("8ec90002-f315-4f60-9fb8-838830daea50")
        val NUS_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val NUS_RX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        val CURRENT_TIME_SERVICE = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb")
        val CURRENT_TIME_CHARACTERISTIC = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb")

        val HARD_FORBIDDEN: Set<UUID> =
            Collections.unmodifiableSet(
                setOf(FE59_DFU_SERVICE, DFU_CONTROL_POINT, DFU_PACKET, NUS_SERVICE, NUS_RX),
            )
        val DEFAULT = AccessPolicy()
    }
}
