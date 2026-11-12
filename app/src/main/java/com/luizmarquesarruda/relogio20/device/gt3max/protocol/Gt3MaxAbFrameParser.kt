package com.luizmarquesarruda.relogio20.device.gt3max.protocol

import java.util.UUID

/**
 * Fingerprint observed on the GT3 MAX and in the public DT78 BLE emulator.
 * This is a compatibility candidate, not proof of the watch chipset/model.
 */
internal object Gt3MaxProtocolProfile {
    val SERVICE_UUID: UUID = Gt3MaxCallDeviceProfile.NUS_SERVICE_UUID
    val RX_CHARACTERISTIC_UUID: UUID = Gt3MaxCallDeviceProfile.NUS_RX_CHARACTERISTIC_UUID
    val TX_CHARACTERISTIC_UUID: UUID = Gt3MaxCallDeviceProfile.NUS_TX_CHARACTERISTIC_UUID

    const val PROFILE_ID = "GT3MAX_NUS_AB_FRAME_CANDIDATE"
}

internal data class Gt3MaxAbFrame(
    val declaredLength: Int,
    val command: Int,
    val response: Int,
    val payload: ByteArray,
) {
    /**
     * 0x91/0x80 with a final value in 0..100 matches the published DT78
     * battery-notification example. It remains a candidate interpretation
     * until repeated against the actual watch under controlled conditions.
     */
    val batteryPercent: Int?
        get() {
            if (command != 0x91 || response != 0x80) return null
            val value = payload.lastOrNull()?.toInt()?.and(0xFF) ?: return null
            return value.takeIf { it in 0..100 }
        }
}

internal object Gt3MaxAbFrameParser {
    private const val HEADER = 0xAB
    private const val FRAME_MARKER = 0xFF
    private const val MINIMUM_DECLARED_LENGTH = 4

    /**
     * Parses exactly one complete frame. Fragment reassembly and concatenated
     * frames are intentionally not inferred from the current single capture.
     */
    fun parse(bytes: ByteArray): Gt3MaxAbFrame? {
        if (bytes.size < 7) return null
        if ((bytes[0].toInt() and 0xFF) != HEADER) return null
        if ((bytes[3].toInt() and 0xFF) != FRAME_MARKER) return null

        val declaredLength =
            ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF)
        if (declaredLength < MINIMUM_DECLARED_LENGTH) return null
        if (bytes.size != declaredLength + 3) return null

        return Gt3MaxAbFrame(
            declaredLength = declaredLength,
            command = bytes[4].toInt() and 0xFF,
            response = bytes[5].toInt() and 0xFF,
            payload = bytes.copyOfRange(6, bytes.size),
        )
    }
}
