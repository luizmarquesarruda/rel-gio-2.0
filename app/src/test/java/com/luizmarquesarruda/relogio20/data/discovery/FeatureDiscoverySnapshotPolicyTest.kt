package com.luizmarquesarruda.relogio20.data.discovery

import com.luizmarquesarruda.relogio20.device.discovery.CharacteristicTraffic
import com.luizmarquesarruda.relogio20.device.discovery.DiscoverySnapshot
import com.luizmarquesarruda.relogio20.device.discovery.FeatureFinding
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureDiscoverySnapshotPolicyTest {
    private fun validSnapshot(
        findings: List<FeatureFinding> =
            listOf(
                FeatureFinding("f1", "Dispositivo", "Bateria", "OBSERVADO", "UUID encontrado", "GATT"),
            ),
        traffic: List<CharacteristicTraffic> =
            listOf(
                CharacteristicTraffic("180f", "2a19", 1, 1_000L, 1, batteryCandidatePercent = 80),
            ),
    ) = DiscoverySnapshot(
        updatedAtMillis = 1_000L,
        serviceCount = 1,
        characteristicCount = 1,
        findings = findings,
        traffic = traffic,
    )

    @Test
    fun acceptsAWellFormedSnapshot() {
        assertTrue(FeatureDiscoverySnapshotPolicy.isValid(validSnapshot()))
    }

    @Test
    fun rejectsDuplicateFindingIds() {
        val finding = FeatureFinding("same", "Dispositivo", "Bateria", "OBSERVADO", "x", "y")
        assertFalse(FeatureDiscoverySnapshotPolicy.isValid(validSnapshot(findings = listOf(finding, finding))))
    }

    @Test
    fun rejectsOutOfRangeBatteryCandidate() {
        val traffic = CharacteristicTraffic("180f", "2a19", 1, 1_000L, 1, batteryCandidatePercent = 101)
        assertFalse(FeatureDiscoverySnapshotPolicy.isValid(validSnapshot(traffic = listOf(traffic))))
    }

    @Test
    fun rejectsNegativeCountsAndOutOfRangeTimestamps() {
        val negativeTraffic = CharacteristicTraffic("180f", "2a19", -1, 1_000L, 1)
        assertFalse(FeatureDiscoverySnapshotPolicy.isValid(validSnapshot(traffic = listOf(negativeTraffic))))

        val expiredTimestamp =
            validSnapshot().copy(
                updatedAtMillis = FeatureDiscoverySnapshotPolicy.MAX_TIMESTAMP + 1L,
            )
        assertFalse(FeatureDiscoverySnapshotPolicy.isValid(expiredTimestamp))
    }

    @Test
    fun rejectsDuplicateTrafficKeys() {
        val item = CharacteristicTraffic("180f", "2a19", 1, 1_000L, 1)
        assertFalse(FeatureDiscoverySnapshotPolicy.isValid(validSnapshot(traffic = listOf(item, item))))
    }
}
