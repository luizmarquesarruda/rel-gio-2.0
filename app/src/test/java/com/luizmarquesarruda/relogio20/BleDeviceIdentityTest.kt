package com.luizmarquesarruda.relogio20

import com.luizmarquesarruda.relogio20.device.ble.core.BleDeviceIdentity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BleDeviceIdentityTest {
    @Test fun normalizesAddress() {
        assertEquals("C0:27:AB:64:6C:E2", BleDeviceIdentity.normalize(" c0:27:ab:64:6c:e2 "))
    }
    @Test fun rejectsInvalidAddress() {
        assertEquals(null, BleDeviceIdentity.normalize("GT3 MAX"))
        assertEquals(null, BleDeviceIdentity.normalize(null))
    }
    @Test fun requiresExactMatch() {
        assertTrue(BleDeviceIdentity.matches("C0:27:AB:64:6C:E2", "c0:27:ab:64:6c:e2"))
        assertFalse(BleDeviceIdentity.matches("C0:27:AB:64:6C:E2", "C0:27:AB:60:FE:06"))
        assertFalse(BleDeviceIdentity.matches(null, "C0:27:AB:64:6C:E2"))
    }
}
