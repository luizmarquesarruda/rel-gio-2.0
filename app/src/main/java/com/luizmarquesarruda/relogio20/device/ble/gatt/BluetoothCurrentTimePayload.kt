package com.luizmarquesarruda.relogio20.device.ble.gatt

import java.time.LocalDateTime

/**
 * Encodes the Bluetooth SIG Current Time characteristic (CTS 0x1805 / 0x2A2B).
 * This is the peripheral's local date/time, not UTC.
 */
internal object BluetoothCurrentTimePayload {
    const val SERVICE_UUID = "00001805-0000-1000-8000-00805f9b34fb"
    const val CURRENT_TIME_UUID = "00002a2b-0000-1000-8000-00805f9b34fb"
    const val BYTE_LENGTH = 10
    const val EXTERNAL_REFERENCE_UPDATE = 0x02

    fun encode(
        localDateTime: LocalDateTime,
        adjustReason: Int = EXTERNAL_REFERENCE_UPDATE,
    ): ByteArray {
        require(localDateTime.year in 1..0xFFFF) { "Year does not fit CTS uint16 field." }
        require(adjustReason in 0..0x0F) { "CTS adjust reason contains unsupported bits." }
        val year = localDateTime.year
        return byteArrayOf(
            (year and 0xFF).toByte(),
            ((year ushr 8) and 0xFF).toByte(),
            localDateTime.monthValue.toByte(),
            localDateTime.dayOfMonth.toByte(),
            localDateTime.hour.toByte(),
            localDateTime.minute.toByte(),
            localDateTime.second.toByte(),
            localDateTime.dayOfWeek.value.toByte(),
            0,
            adjustReason.toByte(),
        )
    }
}
