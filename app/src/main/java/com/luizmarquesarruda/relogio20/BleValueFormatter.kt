package com.luizmarquesarruda.relogio20

internal object BleValueFormatter {
    fun toHex(bytes: ByteArray): String {
        if (bytes.isEmpty()) return "(vazio)"
        return bytes.joinToString("-") { byte ->
            String.format("%02X", byte.toInt() and 0xFF)
        }
    }
}
