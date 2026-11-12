package com.luizmarquesarruda.relogio20.device.ble.core

internal object BleValueFormatter {
    fun toHex(bytes: ByteArray): String {
        if (bytes.isEmpty()) return "(vazio)"
        return bytes.joinToString("-") { byte ->
            String.format("%02X", byte.toInt() and 0xFF)
        }
    }
}
