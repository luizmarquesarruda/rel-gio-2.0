package com.luizmarquesarruda.relogio20

import com.luizmarquesarruda.relogio20.device.ble.core.BleScanCandidatePolicy

import com.luizmarquesarruda.relogio20.device.gt3max.protocol.Gt3MaxCallDeviceProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class BleScanCandidatePolicyTest {
    @Test fun savedAddressIsPreferredEvenWithoutNameOrServices() {
        assertEquals(BleScanCandidatePolicy.Match.SAVED_ADDRESS,
            BleScanCandidatePolicy.evaluate("C0:27:AB:64:6C:E2", "c0:27:ab:64:6c:e2", null, emptySet()))
    }

    @Test fun recognizesKnownNameWithoutManualAddress() {
        assertEquals(BleScanCandidatePolicy.Match.DEVICE_NAME,
            BleScanCandidatePolicy.evaluate(null, "AA:BB:CC:DD:EE:FF", "GT3 MAX", emptySet()))
    }

    @Test fun recognizesAdvertisedNusWithoutName() {
        assertEquals(BleScanCandidatePolicy.Match.ADVERTISED_NUS,
            BleScanCandidatePolicy.evaluate(null, "AA:BB:CC:DD:EE:FF", null,
                setOf(Gt3MaxCallDeviceProfile.NUS_SERVICE_UUID.toString())))
    }

    @Test fun rejectsUnrelatedNameWithoutKnownServices() {
        assertEquals(BleScanCandidatePolicy.Match.OTHER,
            BleScanCandidatePolicy.evaluate(null, "AA:BB:CC:DD:EE:FF", "JBL Speaker", emptySet()))
    }
}
