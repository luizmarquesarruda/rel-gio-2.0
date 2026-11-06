package com.luizmarquesarruda.relogio20

import com.luizmarquesarruda.relogio20.device.ble.core.BleValueFormatter

import org.junit.Assert.assertEquals
import org.junit.Test

class BleValueFormatterTest {
    @Test
    fun formatsUnsignedBytesAsUppercaseHex() {
        val input = byteArrayOf(0xAB.toByte(), 0x00, 0x0C)
        assertEquals("AB-00-0C", BleValueFormatter.toHex(input))
    }

    @Test
    fun identifiesEmptyPayloadWithoutInventingAValue() {
        assertEquals("(vazio)", BleValueFormatter.toHex(byteArrayOf()))
    }
}
