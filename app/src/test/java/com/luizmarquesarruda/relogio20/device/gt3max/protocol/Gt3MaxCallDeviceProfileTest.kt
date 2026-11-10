package com.luizmarquesarruda.relogio20.device.gt3max.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class Gt3MaxCallDeviceProfileTest {
    private fun observedServices(): Map<UUID, Set<UUID>> =
        mapOf(
            Gt3MaxCallDeviceProfile.NUS_SERVICE_UUID to
                setOf(
                    Gt3MaxCallDeviceProfile.NUS_RX_CHARACTERISTIC_UUID,
                    Gt3MaxCallDeviceProfile.NUS_TX_CHARACTERISTIC_UUID,
                ),
            UUID.fromString("00003802-0000-1000-8000-00805f9b34fb") to
                setOf(
                    UUID.fromString("00004a02-0000-1000-8000-00805f9b34fb"),
                ),
            UUID.fromString("cc353442-be58-4ea2-876e-11d8d6976366") to
                setOf(
                    UUID.fromString("c551c36a-0377-4a29-9657-74ffb655a188"),
                    UUID.fromString("00008a8f-0000-1000-8000-00805f9b34fb"),
                ),
            UUID.fromString("00003803-0000-1000-8000-00805f9b34fb") to
                setOf(
                    UUID.fromString("00004a03-0000-1000-8000-00805f9b34fb"),
                ),
        )

    @Test
    fun recognizesBothObservedAdvertisedNameVariants() {
        assertTrue(Gt3MaxCallDeviceProfile.isKnownDeviceName("GT3 MAX"))
        assertTrue(Gt3MaxCallDeviceProfile.isKnownDeviceName("GT3 MAX Call"))
        assertTrue(Gt3MaxCallDeviceProfile.isKnownDeviceName("gt3-max"))
        assertFalse(Gt3MaxCallDeviceProfile.isKnownDeviceName("GT4 MAX"))
    }

    @Test
    fun requiresNusRxAndTxForNusTransport() {
        val incomplete =
            mapOf(
                Gt3MaxCallDeviceProfile.NUS_SERVICE_UUID to
                    setOf(Gt3MaxCallDeviceProfile.NUS_TX_CHARACTERISTIC_UUID),
            )
        assertFalse(Gt3MaxCallDeviceProfile.hasNusTransport(incomplete))
    }

    @Test
    fun recognizesTheCapturedGattFingerprintWithoutCallingItFirmwareAuthentication() {
        assertEquals(
            Gt3MaxCallDeviceProfile.Match.CAPTURED_GATT_FINGERPRINT,
            Gt3MaxCallDeviceProfile.evaluate("GT3 MAX", observedServices()),
        )
    }

    @Test
    fun autoSubscriptionRequiresCapturedFingerprintAndNusEvenWhenNameIsMissing() {
        assertTrue(Gt3MaxCallDeviceProfile.shouldAutoSubscribeBattery("GT3 MAX", observedServices()))
        assertTrue(Gt3MaxCallDeviceProfile.shouldAutoSubscribeBattery(null, observedServices()))
        assertFalse(
            Gt3MaxCallDeviceProfile.shouldAutoSubscribeBattery(
                "Unknown watch",
                mapOf(
                    Gt3MaxCallDeviceProfile.NUS_SERVICE_UUID to
                        setOf(
                            Gt3MaxCallDeviceProfile.NUS_RX_CHARACTERISTIC_UUID,
                            Gt3MaxCallDeviceProfile.NUS_TX_CHARACTERISTIC_UUID,
                        ),
                ),
            ),
        )
    }

    @Test
    fun doesNotAutoSubscribeWhenOnlyNameAndNusMatchWithoutCapturedCustomServices() {
        val partial =
            mapOf(
                Gt3MaxCallDeviceProfile.NUS_SERVICE_UUID to
                    setOf(
                        Gt3MaxCallDeviceProfile.NUS_RX_CHARACTERISTIC_UUID,
                        Gt3MaxCallDeviceProfile.NUS_TX_CHARACTERISTIC_UUID,
                    ),
            )
        assertFalse(
            Gt3MaxCallDeviceProfile.shouldAutoSubscribeBattery("GT3 MAX", partial),
        )
    }

    @Test
    fun firmwareMetadataComesFromOwnerReport() {
        assertEquals("V1.07", Gt3MaxCallDeviceProfile.REPORTED_FIRMWARE_VERSION)
        assertEquals(
            "SH0050-1_1.1.3_1.23_1.18_1.1",
            Gt3MaxCallDeviceProfile.REPORTED_SOFTWARE_BUILD,
        )
    }
}
