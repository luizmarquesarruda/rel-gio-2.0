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
            "180d", "2a37", byteArrayOf(0x02, 72)
        )
        assertEquals("Sem contato detectado", notDetected.last().value)

        val detected = GattValueDecoder.decode(
            "180d", "2a37", byteArrayOf(0x06, 72)
        )
        assertEquals("Contato detectado", detected.last().value)
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
