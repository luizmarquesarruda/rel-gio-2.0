package com.luizmarquesarruda.relogio20.device.ble.gatt

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDateTime

class BluetoothCurrentTimePayloadTest {
    @Test
    fun encodesTenByteCtsValueWithLittleEndianYearAndIsoWeekday() {
        val value =
            BluetoothCurrentTimePayload.encode(
                LocalDateTime.of(2026, 11, 4, 10, 16, 2),
            )
        assertEquals(BluetoothCurrentTimePayload.BYTE_LENGTH, value.size)
        assertArrayEquals(
            byteArrayOf(
                0xEA.toByte(),
                0x07,
                0x0B,
                0x04,
                0x0A,
                0x10,
                0x02,
                0x03,
                0x00,
                0x02,
            ),
            value,
        )
    }

    @Test
    fun permitsAnExplicitZeroAdjustReason() {
        val value =
            BluetoothCurrentTimePayload.encode(
                LocalDateTime.of(2026, 1, 1, 0, 0, 0),
                adjustReason = 0,
            )
        assertEquals(0, value[9].toInt())
    }

    @Test
    fun rejectsAdjustReasonBitsOutsideTheCtsField() {
        assertThrows(IllegalArgumentException::class.java) {
            BluetoothCurrentTimePayload.encode(
                LocalDateTime.of(2026, 1, 1, 0, 0, 0),
                adjustReason = 0x10,
            )
        }
    }
}
