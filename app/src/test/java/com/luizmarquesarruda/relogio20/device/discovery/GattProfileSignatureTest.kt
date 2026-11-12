package com.luizmarquesarruda.relogio20.device.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GattProfileSignatureTest {
    @Test
    fun signatureIsIndependentOfServiceCharacteristicAndPropertyOrder() {
        val first =
            listOf(
                GattServiceRecord(
                    "0000180f-0000-1000-8000-00805f9b34fb",
                    listOf(
                        GattCharacteristicRecord(
                            "00002a19-0000-1000-8000-00805f9b34fb",
                            setOf("READ", "NOTIFY"),
                            setOf("00002902-0000-1000-8000-00805f9b34fb"),
                        ),
                    ),
                ),
                GattServiceRecord(
                    "0000180d-0000-1000-8000-00805f9b34fb",
                    listOf(
                        GattCharacteristicRecord(
                            "00002a37-0000-1000-8000-00805f9b34fb",
                            setOf("NOTIFY"),
                        ),
                    ),
                ),
            )
        val reordered =
            listOf(
                GattServiceRecord(
                    "0000180d-0000-1000-8000-00805f9b34fb",
                    listOf(
                        GattCharacteristicRecord(
                            "00002a37-0000-1000-8000-00805f9b34fb",
                            setOf("NOTIFY"),
                        ),
                    ),
                ),
                GattServiceRecord(
                    "0000180f-0000-1000-8000-00805f9b34fb",
                    listOf(
                        GattCharacteristicRecord(
                            "00002a19-0000-1000-8000-00805f9b34fb",
                            setOf("NOTIFY", "READ"),
                            setOf("00002902-0000-1000-8000-00805f9b34fb"),
                        ),
                    ),
                ),
            )

        assertEquals(
            GattProfileSignature.calculate(first).sha256,
            GattProfileSignature.calculate(reordered).sha256,
        )
    }

    @Test
    fun changingGattPropertiesChangesTheSignature() {
        val readable =
            listOf(
                GattServiceRecord(
                    "0000180f-0000-1000-8000-00805f9b34fb",
                    listOf(
                        GattCharacteristicRecord(
                            "00002a19-0000-1000-8000-00805f9b34fb",
                            setOf("READ"),
                        ),
                    ),
                ),
            )
        val notifiable =
            listOf(
                GattServiceRecord(
                    "0000180f-0000-1000-8000-00805f9b34fb",
                    listOf(
                        GattCharacteristicRecord(
                            "00002a19-0000-1000-8000-00805f9b34fb",
                            setOf("NOTIFY"),
                        ),
                    ),
                ),
            )

        assertNotEquals(
            GattProfileSignature.calculate(readable).sha256,
            GattProfileSignature.calculate(notifiable).sha256,
        )
    }

    @Test
    fun comparisonDistinguishesFirstCaptureUnchangedAndChanged() {
        val services =
            listOf(
                GattServiceRecord(
                    "0000180f-0000-1000-8000-00805f9b34fb",
                    listOf(
                        GattCharacteristicRecord(
                            "00002a19-0000-1000-8000-00805f9b34fb",
                            setOf("READ"),
                        ),
                    ),
                ),
            )
        val signature = GattProfileSignature.calculate(services)
        val changed = GattProfileSignature.calculate(emptyList())

        assertEquals(
            GattProfileSignature.Change.FIRST_CAPTURE,
            GattProfileSignature.compare(signature, null),
        )
        assertEquals(
            GattProfileSignature.Change.UNCHANGED,
            GattProfileSignature.compare(signature, signature.sha256),
        )
        assertEquals(
            GattProfileSignature.Change.CHANGED,
            GattProfileSignature.compare(changed, signature.sha256),
        )
        assertEquals(1, signature.serviceCount)
        assertEquals(1, signature.characteristicCount)
    }

    @Test
    fun invalidPreviousHashIsTreatedAsFirstCapture() {
        val signature = GattProfileSignature.calculate(emptyList())
        assertEquals(
            GattProfileSignature.Change.FIRST_CAPTURE,
            GattProfileSignature.compare(signature, "not-a-sha"),
        )
    }
}
