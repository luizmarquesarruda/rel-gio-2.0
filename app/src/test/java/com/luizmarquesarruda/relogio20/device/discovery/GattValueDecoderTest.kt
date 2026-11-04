package com.luizmarquesarruda.relogio20.device.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GattValueDecoderTest {
    @Test
    fun decodesBatteryLevelOnlyWhenPayloadIsOneValidPercentageByte() {
        val decoded = GattValueDecoder.decode("180f", "2a19", byteArrayOf(87))
        assertEquals("Bateria", decoded.single().metric)
        assertEquals("87%", decoded.single().value)
        assertTrue(GattValueDecoder.decode("180f", "2a19", byteArrayOf(101)).isEmpty())
        assertTrue(GattValueDecoder.decode("180f", "2a19", byteArrayOf(87, 0)).isEmpty())
    }

    @Test
    fun decodesEightAndSixteenBitHeartRateMeasurements() {
        val eightBit = GattValueDecoder.decode("180d", "2a37", byteArrayOf(0, 72))
        assertEquals("72 bpm", eightBit.single().value)

        val sixteenBit = GattValueDecoder.decode("180d", "2a37", byteArrayOf(1, 0x2c, 0x01))
        assertEquals("300 bpm", sixteenBit.single().value)
    }

    @Test
    fun decodesBloodPressureCompoundSfloatValues() {
        val data = byteArrayOf(0, 0x78, 0, 0x4b, 0, 0x5a, 0)
        val decoded = GattValueDecoder.decode("1810", "2a35", data)
        assertEquals("Pressão arterial", decoded.single().metric)
        assertTrue(decoded.single().value.contains("120.0"))
        assertTrue(decoded.single().value.contains("75.0"))
        assertTrue(decoded.single().value.contains("90.0"))
        assertTrue(decoded.single().value.contains("mmHg"))
    }

    @Test
    fun decodesHealthThermometerIeee11073Float() {
        val data = byteArrayOf(0, 0x6d, 0x01, 0, 0xff.toByte())
        val decoded = GattValueDecoder.decode("1809", "2a1c", data)
        assertEquals("Temperatura do termômetro", decoded.single().metric)
        assertEquals("36.50 °C", decoded.single().value)
    }

    @Test
    fun decodesEnvironmentalTemperatureAndHumidity() {
        val temperature = GattValueDecoder.decode("181a", "2a6e", byteArrayOf(0x6d, 0xf1.toByte()))
        assertEquals("Temperatura ambiente", temperature.single().metric)
        assertEquals("36.50 °C", temperature.single().value)

        val humidity = GattValueDecoder.decode("181a", "2a6f", byteArrayOf(0x10, 0x27))
        assertEquals("100.00 %", humidity.single().value)
    }

    @Test
    fun decodesSpotCheckAndContinuousOximetryFrames() {
        val spot = GattValueDecoder.decode(
            "1822", "2a5e",
            byteArrayOf(0, 0x62, 0, 0x48, 0)
        )
        assertEquals(2, spot.size)
        assertEquals("98.00 %", spot[0].value)
        assertEquals("72.00 bpm", spot[1].value)

        val continuous = GattValueDecoder.decode(
            "1822", "2a5f",
            byteArrayOf(0, 0x62, 0, 0x48, 0)
        )
        assertEquals(2, continuous.size)
        assertEquals("98.00 %", continuous[0].value)
    }

    @Test
    fun malformedAndUnknownFramesRemainUninterpreted() {
        assertTrue(GattValueDecoder.decode("1810", "2a35", byteArrayOf(0, 1)).isEmpty())
        assertTrue(GattValueDecoder.decode("12345678-1234-5678-1234-56789abcdef0", "abcd", byteArrayOf(1, 2)).isEmpty())
        assertTrue(GattValueDecoder.decode("180d", "2a37", byteArrayOf()).isEmpty())
    }
    @Test
    fun decodesHeartRateSensorContactFlagsCorrectly() {
        val notDetected = GattValueDecoder.decode(
            "180d", "2a37", byteArrayOf(0x04, 72)
        )
        assertEquals("Sem contato detectado", notDetected.last().value)

        val detected = GattValueDecoder.decode(
            "180d", "2a37", byteArrayOf(0x06, 72)
        )
        assertEquals("Contato detectado", detected.last().value)

        // Status bit without the support bit must not be interpreted as contact.
        val unsupported = GattValueDecoder.decode(
            "180d", "2a37", byteArrayOf(0x02, 72)
        )
        assertEquals(1, unsupported.size)
    }


    @Test
    fun decodesIntermediateCuffPressureOnlyWithRequiredNaNPlaceholders() {
        val valid = byteArrayOf(0, 0x78, 0, 0xff.toByte(), 0x07, 0xff.toByte(), 0x07)
        val decoded = GattValueDecoder.decode("1810", "2a36", valid)
        assertEquals("Pressão intermediária da braçadeira", decoded.single().metric)
        assertEquals("120.0 mmHg", decoded.single().value)

        val malformed = byteArrayOf(0, 0x78, 0, 0, 0, 0xff.toByte(), 0x07)
        assertTrue(GattValueDecoder.decode("1810", "2a36", malformed).isEmpty())
    }

    @Test
    fun decodesEnvironmentalPressureAndBodySensorLocation() {
        val pressure = GattValueDecoder.decode(
            "181a", "2a6d", byteArrayOf(0xe8.toByte(), 0x13)
        )
        assertEquals("Pressão atmosférica", pressure.single().metric)
        assertEquals("10000.00 Pa", pressure.single().value)

        val location = GattValueDecoder.decode("180d", "2a38", byteArrayOf(0x02))
        assertEquals("Localização declarada do sensor", location.single().metric)
        assertEquals("Pulso", location.single().value)
    }

    @Test
    fun decodesRscAndCscMeasurementsWithoutInventingDerivedSpeedOrCadence() {
        val rsc = GattValueDecoder.decode(
            "1814", "2a53",
            byteArrayOf(0x07, 0x00, 0x01, 90, 0x2c, 0x01, 0x64, 0, 0, 0)
        )
        assertTrue(rsc.any { it.metric == "Velocidade instantânea RSC" && it.value == "1.000 m/s" })
        assertTrue(rsc.any { it.metric == "Cadência instantânea RSC" && it.value == "90 passos/min" })
        assertTrue(rsc.any { it.metric == "Comprimento de passada RSC" && it.value == "3.00 m" })
        assertTrue(rsc.any { it.metric == "Distância total RSC" && it.value == "10.0 m" })
        assertTrue(rsc.any { it.metric == "Estado de deslocamento RSC" && it.value == "corrida" })

        val walking = GattValueDecoder.decode(
            "1814", "2a53",
            byteArrayOf(0x00, 0x00, 0x01, 90)
        )
        assertTrue(walking.any { it.metric == "Estado de deslocamento RSC" && it.value == "caminhada" })

        val csc = GattValueDecoder.decode(
            "1816", "2a5b",
            byteArrayOf(0x03, 0x10, 0, 0, 0, 0x00, 0x04, 0x04, 0x00, 0x00, 0x00)
        )
        assertTrue(csc.any { it.metric == "Revoluções acumuladas da roda CSC" && it.value == "16" })
        assertTrue(csc.any { it.metric == "Revoluções acumuladas do pedivela CSC" && it.value == "4" })
    }


    @Test
    fun decodesPulseOximeterFeaturesAndOptionalSupportBitfields() {
        val base = GattValueDecoder.decode("1822", "2a60", byteArrayOf(0xfc.toByte(), 0))
        assertEquals(8, base.size)
        assertTrue(base.any {
            it.metric == "Recurso de oximetria • Suporte a status da medição" &&
                it.value == "suportado"
        })

        val extended = GattValueDecoder.decode(
            "1822", "2a60",
            byteArrayOf(0x03, 0, 0x20, 0, 0x00, 0x02, 0)
        )
        assertTrue(extended.any {
            it.metric == "Recurso de oximetria • Status da medição bit 5" &&
                it.value == "suportado"
        })
        assertTrue(extended.any {
            it.metric == "Recurso de oximetria • Status do dispositivo/sensor bit 9" &&
                it.value == "suportado"
        })
        assertTrue(GattValueDecoder.decode("1822", "2a60", byteArrayOf(0x01, 0)).isEmpty())
    }


    @Test
    fun decodesBodyCompositionFeatureFlagsAndResolution() {
        val flags = (1L shl 0) or (1L shl 9) or (7L shl 11) or (3L shl 15)
        val data = byteArrayOf(
            flags.toByte(),
            (flags ushr 8).toByte(),
            (flags ushr 16).toByte(),
            (flags ushr 24).toByte()
        )
        val decoded = GattValueDecoder.decode("181b", "2a9b", data)
        assertTrue(decoded.any {
            it.metric == "Recurso de composição corporal • Timestamp" &&
                it.value == "suportado"
        })
        assertTrue(decoded.any {
            it.metric == "Resolução declarada de peso" &&
                it.value == "0,005 kg / 0,01 lb"
        })
        assertTrue(decoded.any {
            it.metric == "Resolução declarada de altura" &&
                it.value == "0,001 m / 0,1 in"
        })
        assertTrue(GattValueDecoder.decode("181b", "2a9b", byteArrayOf(1, 0, 0)).isEmpty())
    }

    @Test
    fun decodesDeviceInformationFirmwareString() {
        val decoded = GattValueDecoder.decode(
            "180a", "2a26", "V1.07".toByteArray(Charsets.UTF_8)
        )
        assertEquals("Versão de firmware anunciada", decoded.single().metric)
        assertEquals("V1.07", decoded.single().value)
    }

}
