package com.luizmarquesarruda.relogio20.device.discovery

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import kotlin.math.pow

internal data class DecodedGattValue(
    val metric: String,
    val value: String,
    val source: String = "BLUETOOTH_SIG"
)

/**
 * Decodes only characteristics whose field layout is defined by Bluetooth SIG.
 * An accepted frame format is not proof of clinical accuracy or sensor calibration.
 * Unknown/proprietary payloads are deliberately left uninterpreted.
 */
internal object GattValueDecoder {
    fun decode(
        serviceUuid: String,
        characteristicUuid: String,
        payload: ByteArray
    ): List<DecodedGattValue> {
        if (payload.isEmpty()) return emptyList()
        val service = shortUuid(serviceUuid)
        val characteristic = shortUuid(characteristicUuid)

        return when {
            service == "180f" && characteristic == "2a19" ->
                decodeBattery(payload)
            service == "180a" ->
                decodeDeviceInformation(characteristic, payload)
            service == "180d" && characteristic == "2a37" ->
                decodeHeartRate(payload)
            service == "1810" && characteristic == "2a35" ->
                decodeBloodPressure(payload)
            service == "1809" && characteristic == "2a1c" ->
                decodeHealthThermometer(payload)
            service == "1809" && characteristic == "2a1e" ->
                decodeIntermediateTemperature(payload)
            service == "181a" && characteristic == "2a6e" ->
                decodeEnvironmentalTemperature(payload)
            service == "181a" && characteristic == "2a6f" ->
                decodeHumidity(payload)
            service == "1822" && characteristic == "2a5e" ->
                decodePulseOximeterSpotCheck(payload)
            service == "1822" && characteristic == "2a5f" ->
                decodePulseOximeterContinuous(payload)
            else -> emptyList()
        }
    }

    private fun decodeBattery(value: ByteArray): List<DecodedGattValue> {
        if (value.size != 1) return emptyList()
        val level = value[0].toInt() and 0xff
        if (level !in 0..100) return emptyList()
        return listOf(DecodedGattValue("Bateria", "$level%"))
    }

    private fun decodeDeviceInformation(
        characteristic: String,
        value: ByteArray
    ): List<DecodedGattValue> {
        val label = when (characteristic) {
            "2a29" -> "Fabricante anunciado"
            "2a24" -> "Modelo anunciado"
            "2a26" -> "Versão de firmware anunciada"
            "2a27" -> "Versão de hardware anunciada"
            "2a28" -> "Versão de software anunciada"
            else -> return emptyList()
        }
        val decoded = value.toString(Charsets.UTF_8)
            .trim('\u0000', ' ', '\r', '\n', '\t')
            .takeIf { text -> text.isNotBlank() && text.none { it.isISOControl() } }
            ?: return emptyList()
        return listOf(DecodedGattValue(label, decoded, "METADADO_GATT_OBSERVADO"))
    }

    private fun decodeHeartRate(value: ByteArray): List<DecodedGattValue> {
        if (value.size < 2) return emptyList()
        val flags = value[0].toInt() and 0xff
        var offset = 1
        val isUInt16 = flags and 0x01 != 0
        val heartRate = if (isUInt16) {
            if (value.size < offset + 2) return emptyList()
            readUInt16(value, offset).also { offset += 2 }
        } else {
            value[offset++].toInt() and 0xff
        }
        val results = mutableListOf(
            DecodedGattValue("Frequência cardíaca", "$heartRate bpm")
        )

        val contactBits = (flags ushr 1) and 0x03
        // Bit 2 = contact supported; bit 1 = contact detected.
        // 0b10 means supported without contact; 0b11 means supported with contact.
        when (contactBits) {
            0x02 -> results += DecodedGattValue("Contato do sensor", "Sem contato detectado")
            0x03 -> results += DecodedGattValue("Contato do sensor", "Contato detectado")
        }

        if (flags and 0x08 != 0) {
            if (value.size < offset + 2) return emptyList()
            val energy = readUInt16(value, offset)
            results += DecodedGattValue("Energia acumulada do sensor", "$energy kJ")
            offset += 2
        }
        if (flags and 0x10 != 0) {
            val rrCount = (value.size - offset) / 2
            if (rrCount == 0 || (value.size - offset) % 2 != 0) return emptyList()
            val rrIntervals = (0 until rrCount).map { index ->
                readUInt16(value, offset + index * 2) / 1024.0
            }
            results += DecodedGattValue(
                "Intervalos RR",
                rrIntervals.joinToString(", ") { String.format(Locale.US, "%.3f s", it) }
            )
        }
        return results
    }

    private fun decodeBloodPressure(value: ByteArray): List<DecodedGattValue> {
        if (value.size < 7) return emptyList()
        val flags = value[0].toInt() and 0xff
        var offset = 1
        val systolic = readSFloat(value, offset) ?: return emptyList()
        offset += 2
        val diastolic = readSFloat(value, offset) ?: return emptyList()
        offset += 2
        val mean = readSFloat(value, offset) ?: return emptyList()
        offset += 2
        val unit = if (flags and 0x01 != 0) "kPa" else "mmHg"
        val results = mutableListOf(
            DecodedGattValue(
                "Pressão arterial",
                String.format(
                    Locale.US,
                    "sistólica %.1f • diastólica %.1f • média %.1f %s",
                    systolic, diastolic, mean, unit
                )
            )
        )

        if (flags and 0x02 != 0) {
            val timestamp = decodeDateTime(value, offset) ?: return emptyList()
            results += DecodedGattValue("Horário da pressão", timestamp)
            offset += 7
        }
        if (flags and 0x04 != 0) {
            val pulse = readSFloat(value, offset) ?: return emptyList()
            results += DecodedGattValue(
                "Pulso informado pela medição de pressão",
                String.format(Locale.US, "%.1f bpm", pulse)
            )
            offset += 2
        }
        if (flags and 0x08 != 0) {
            if (value.size < offset + 1) return emptyList()
            results += DecodedGattValue("Identificador de usuário do sensor", (value[offset].toInt() and 0xff).toString())
            offset += 1
        }
        if (flags and 0x10 != 0) {
            if (value.size < offset + 2) return emptyList()
            results += DecodedGattValue("Status da medição de pressão", "0x" + readUInt16(value, offset).toString(16).uppercase(Locale.US))
        }
        return results
    }

    private fun decodeHealthThermometer(value: ByteArray): List<DecodedGattValue> =
        decodeTemperatureMeasurement(value, "Temperatura do termômetro")

    private fun decodeIntermediateTemperature(value: ByteArray): List<DecodedGattValue> =
        decodeTemperatureMeasurement(value, "Temperatura intermediária do termômetro")

    private fun decodeTemperatureMeasurement(
        value: ByteArray,
        metric: String
    ): List<DecodedGattValue> {
        if (value.size < 5) return emptyList()
        val flags = value[0].toInt() and 0xff
        val raw = readUInt32(value, 1)
        val temperature = readFloat11073(raw) ?: return emptyList()
        val fahrenheit = flags and 0x01 != 0
        val displayed = if (fahrenheit) temperature else temperature
        val unit = if (fahrenheit) "°F" else "°C"
        val results = mutableListOf(
            DecodedGattValue(metric, String.format(Locale.US, "%.2f %s", displayed, unit))
        )
        var offset = 5
        if (flags and 0x02 != 0) {
            val timestamp = decodeDateTime(value, offset) ?: return emptyList()
            results += DecodedGattValue("Horário da temperatura", timestamp)
            offset += 7
        }
        if (flags and 0x04 != 0) {
            if (value.size < offset + 1) return emptyList()
            results += DecodedGattValue("Tipo de temperatura GATT", (value[offset].toInt() and 0xff).toString())
        }
        return results
    }

    private fun decodeEnvironmentalTemperature(value: ByteArray): List<DecodedGattValue> {
        if (value.size != 2) return emptyList()
        val temperature = readSFloat(value, 0) ?: return emptyList()
        return listOf(
            DecodedGattValue(
                "Temperatura ambiente",
                String.format(Locale.US, "%.2f °C", temperature)
            )
        )
    }

    private fun decodeHumidity(value: ByteArray): List<DecodedGattValue> {
        if (value.size != 2) return emptyList()
        val humidity = readUInt16(value, 0) / 100.0
        if (humidity !in 0.0..100.0) return emptyList()
        return listOf(
            DecodedGattValue(
                "Umidade ambiente",
                String.format(Locale.US, "%.2f %%", humidity)
            )
        )
    }

    private fun decodePulseOximeterSpotCheck(value: ByteArray): List<DecodedGattValue> {
        if (value.size < 5) return emptyList()
        val flags = value[0].toInt() and 0xff
        var offset = 1
        val spo2 = readSFloat(value, offset) ?: return emptyList()
        offset += 2
        val pulse = readSFloat(value, offset) ?: return emptyList()
        offset += 2
        val results = mutableListOf(
            DecodedGattValue("SpO₂ spot-check", formatSFloat(spo2, "%")),
            DecodedGattValue("Pulso spot-check", formatSFloat(pulse, "bpm"))
        )
        if (flags and 0x01 != 0) {
            val timestamp = decodeDateTime(value, offset) ?: return emptyList()
            results += DecodedGattValue("Horário do SpO₂", timestamp)
            offset += 7
        }
        if (flags and 0x02 != 0) {
            if (value.size < offset + 2) return emptyList()
            results += DecodedGattValue("Status da medição de SpO₂", "0x" + readUInt16(value, offset).toString(16).uppercase(Locale.US))
            offset += 2
        }
        if (flags and 0x04 != 0) {
            if (value.size < offset + 3) return emptyList()
            results += DecodedGattValue("Estado do sensor de SpO₂", "0x" + readUInt24(value, offset).toString(16).uppercase(Locale.US))
            offset += 3
        }
        if (flags and 0x08 != 0) {
            val index = readSFloat(value, offset) ?: return emptyList()
            results += DecodedGattValue("Índice de amplitude de pulso", formatSFloat(index, "%"))
        }
        return results
    }

    private fun decodePulseOximeterContinuous(value: ByteArray): List<DecodedGattValue> {
        if (value.size < 5) return emptyList()
        val flags = value[0].toInt() and 0xff
        var offset = 1
        val results = mutableListOf<DecodedGattValue>()
        fun readPair(label: String): Boolean {
            if (value.size < offset + 4) return false
            val spo2 = readSFloat(value, offset) ?: return false
            val pulse = readSFloat(value, offset + 2) ?: return false
            results += DecodedGattValue("SpO₂ $label", formatSFloat(spo2, "%"))
            results += DecodedGattValue("Pulso $label", formatSFloat(pulse, "bpm"))
            offset += 4
            return true
        }
        if (!readPair("contínuo normal")) return emptyList()
        if (flags and 0x01 != 0 && !readPair("contínuo rápido")) return emptyList()
        if (flags and 0x02 != 0 && !readPair("contínuo lento")) return emptyList()
        if (flags and 0x04 != 0) {
            if (value.size < offset + 2) return emptyList()
            results += DecodedGattValue("Status da medição de SpO₂", "0x" + readUInt16(value, offset).toString(16).uppercase(Locale.US))
            offset += 2
        }
        if (flags and 0x08 != 0) {
            if (value.size < offset + 3) return emptyList()
            results += DecodedGattValue("Estado do sensor de SpO₂", "0x" + readUInt24(value, offset).toString(16).uppercase(Locale.US))
            offset += 3
        }
        if (flags and 0x10 != 0) {
            val index = readSFloat(value, offset) ?: return emptyList()
            results += DecodedGattValue("Índice de amplitude de pulso", formatSFloat(index, "%"))
        }
        return results
    }

    private fun readSFloat(value: ByteArray, offset: Int): Double? {
        if (offset < 0 || value.size < offset + 2) return null
        val raw = readUInt16(value, offset)
        if (raw in setOf(0x07fe, 0x07ff, 0x0800, 0x0801, 0x0802)) return null
        val mantissaRaw = raw and 0x0fff
        val mantissa = if (mantissaRaw and 0x0800 != 0) mantissaRaw - 0x1000 else mantissaRaw
        val exponentRaw = (raw ushr 12) and 0x0f
        val exponent = if (exponentRaw and 0x08 != 0) exponentRaw - 0x10 else exponentRaw
        return mantissa * 10.0.pow(exponent)
    }

    private fun readFloat11073(raw: Long): Double? {
        val mantissaRaw = (raw and 0x00ff_ffffL).toInt()
        if (mantissaRaw in setOf(0x007ffffe, 0x007fffff, 0x00800000, 0x00800001, 0x00800002)) return null
        val mantissa = if (mantissaRaw and 0x00800000 != 0) mantissaRaw - 0x01000000 else mantissaRaw
        val exponent = ((raw ushr 24) and 0xff).toByte().toInt()
        return mantissa * 10.0.pow(exponent)
    }

    private fun decodeDateTime(value: ByteArray, offset: Int): String? {
        if (offset < 0 || value.size < offset + 7) return null
        val year = readUInt16(value, offset)
        val month = value[offset + 2].toInt() and 0xff
        val day = value[offset + 3].toInt() and 0xff
        val hour = value[offset + 4].toInt() and 0xff
        val minute = value[offset + 5].toInt() and 0xff
        val second = value[offset + 6].toInt() and 0xff
        return runCatching {
            LocalDateTime.of(
                LocalDate.of(year, month, day),
                LocalTime.of(hour, minute, second)
            ).toString()
        }.getOrNull()
    }

    private fun readUInt16(value: ByteArray, offset: Int): Int =
        (value[offset].toInt() and 0xff) or
            ((value[offset + 1].toInt() and 0xff) shl 8)

    private fun readUInt24(value: ByteArray, offset: Int): Int =
        readUInt16(value, offset) or ((value[offset + 2].toInt() and 0xff) shl 16)

    private fun readUInt32(value: ByteArray, offset: Int): Long =
        (value[offset].toLong() and 0xffL) or
            ((value[offset + 1].toLong() and 0xffL) shl 8) or
            ((value[offset + 2].toLong() and 0xffL) shl 16) or
            ((value[offset + 3].toLong() and 0xffL) shl 24)

    private fun formatSFloat(value: Double, unit: String): String =
        String.format(Locale.US, "%.2f %s", value, unit)

    private fun shortUuid(value: String): String {
        val normalized = value.trim().lowercase(Locale.ROOT)
        return when {
            normalized.length == 4 -> normalized
            normalized.startsWith("0000") && normalized.length >= 8 -> normalized.substring(4, 8)
            else -> normalized
        }
    }
}
