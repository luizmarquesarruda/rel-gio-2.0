package com.luizmarquesarruda.relogio20.device.gt3max.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearfitAbStreamDecoderTest {
    private fun batteryFrame(value: Int): ByteArray = byteArrayOf(
        0xAB.toByte(), 0x00, 0x05, 0xFF.toByte(),
        0x91.toByte(), 0x80.toByte(), 0x00, value.toByte()
    )

    @Test
    fun emitsFrameWhenNotificationContainsCompleteFrame() {
        val decoder = WearfitAbStreamDecoder()
        val result = decoder.append(batteryFrame(55))
        assertEquals(1, result.size)
        assertEquals(55, result.single().batteryPercent)
        assertEquals(0, decoder.bufferedByteCount())
    }

    @Test
    fun reassemblesFrameSplitAcrossNotifications() {
        val decoder = WearfitAbStreamDecoder()
        val bytes = batteryFrame(54)
        assertTrue(decoder.append(bytes.copyOfRange(0, 3)).isEmpty())
        val result = decoder.append(bytes.copyOfRange(3, bytes.size))
        assertEquals(1, result.size)
        assertEquals(54, result.single().batteryPercent)
    }

    @Test
    fun separatesMultipleFramesInOneNotification() {
        val decoder = WearfitAbStreamDecoder()
        val result = decoder.append(batteryFrame(53) + batteryFrame(52))
        assertEquals(2, result.size)
        assertEquals(listOf(53, 52), result.map { it.batteryPercent })
    }

    @Test
    fun resynchronizesAfterNoiseAndMalformedLength() {
        val decoder = WearfitAbStreamDecoder()
        val malformed = byteArrayOf(0x01, 0xAB.toByte(), 0x7F, 0xFF.toByte(), 0x12)
        val result = decoder.append(malformed + batteryFrame(51))
        assertEquals(1, result.size)
        assertEquals(51, result.single().batteryPercent)
    }

    @Test
    fun resetDropsAnIncompleteFrame() {
        val decoder = WearfitAbStreamDecoder()
        decoder.append(batteryFrame(50).copyOfRange(0, 4))
        decoder.reset()
        assertEquals(0, decoder.bufferedByteCount())
        assertTrue(decoder.append(byteArrayOf(0x80.toByte(), 0x00)).isEmpty())
    }
}
