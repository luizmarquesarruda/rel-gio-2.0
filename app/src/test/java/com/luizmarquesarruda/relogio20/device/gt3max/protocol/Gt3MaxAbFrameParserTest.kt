package com.luizmarquesarruda.relogio20.device.gt3max.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class Gt3MaxAbFrameParserTest {
    @Test
    fun parsesTheObservedGt3MaxFrameAsCandidateBatteryValue() {
        val frame = Gt3MaxAbFrameParser.parse(
            byteArrayOf(0xAB.toByte(), 0x00, 0x05, 0xFF.toByte(), 0x91.toByte(),
                0x80.toByte(), 0x00, 0x0C)
        )

        assertNotNull(frame)
        assertEquals(0x91, frame?.command)
        assertEquals(0x80, frame?.response)
        assertEquals(12, frame?.batteryPercent)
    }

    @Test
    fun matchesThePublishedDt78BatteryFixtureWithoutClaimingDeviceIdentity() {
        val frame = Gt3MaxAbFrameParser.parse(
            byteArrayOf(0xAB.toByte(), 0x00, 0x05, 0xFF.toByte(), 0x91.toByte(),
                0x80.toByte(), 0x00, 0x50)
        )

        assertEquals(80, frame?.batteryPercent)
    }

    @Test
    fun rejectsWrongHeaderAndMalformedLength() {
        assertNull(Gt3MaxAbFrameParser.parse(byteArrayOf(0xDF.toByte(), 0, 5, -1, -111, -128, 0, 12)))
        assertNull(Gt3MaxAbFrameParser.parse(
            byteArrayOf(0xAB.toByte(), 0x00, 0x06, 0xFF.toByte(), 0x91.toByte(),
                0x80.toByte(), 0x00, 0x0C)
        ))
    }

    @Test
    fun doesNotInterpretUnknownCommandsAsBattery() {
        val frame = Gt3MaxAbFrameParser.parse(
            byteArrayOf(0xAB.toByte(), 0x00, 0x05, 0xFF.toByte(), 0x92.toByte(),
                0xC0.toByte(), 0x00, 0x0C)
        )

        assertNotNull(frame)
        assertNull(frame?.batteryPercent)
    }

    @Test
    fun rejectsIncompleteFrame() {
        assertNull(Gt3MaxAbFrameParser.parse(
            byteArrayOf(0xAB.toByte(), 0x00, 0x05, 0xFF.toByte(), 0x91.toByte())
        ))
    }
}
