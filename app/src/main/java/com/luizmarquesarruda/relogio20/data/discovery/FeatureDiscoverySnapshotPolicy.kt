package com.luizmarquesarruda.relogio20.data.discovery

import com.luizmarquesarruda.relogio20.device.discovery.DiscoverySnapshot

/**
 * Validates persisted discovery metadata before it can be accepted or written.
 * This policy is pure Kotlin so boundary cases are covered by local unit tests.
 */
internal object FeatureDiscoverySnapshotPolicy {
    const val SCHEMA_VERSION = 1
    const val MAX_FINDINGS = 160
    const val MAX_TRAFFIC = 128
    const val MAX_TIMESTAMP = 253_402_300_799_999L
    private const val MAX_SERVICES = 1_024
    private const val MAX_CHARACTERISTICS = 8_192
    private const val MAX_PAYLOAD_BYTES = 65_535

    fun isValid(snapshot: DiscoverySnapshot): Boolean {
        if (snapshot.schemaVersion != SCHEMA_VERSION) return false
        if (snapshot.updatedAtMillis !in 0L..MAX_TIMESTAMP) return false
        if (snapshot.serviceCount !in 0..MAX_SERVICES) return false
        if (snapshot.characteristicCount !in 0..MAX_CHARACTERISTICS) return false
        if (snapshot.findings.size > MAX_FINDINGS || snapshot.traffic.size > MAX_TRAFFIC) return false

        if (snapshot.findings.any { finding ->
                finding.id.isBlank() || finding.id.length > 256 ||
                    finding.category.isBlank() || finding.category.length > 64 ||
                    finding.title.isBlank() || finding.title.length > 256 ||
                    finding.status.isBlank() || finding.status.length > 128 ||
                    finding.detail.length > 4_096 ||
                    finding.evidence.length > 1_024 ||
                    finding.observationCount < 0
            }
        ) {
            return false
        }
        if (snapshot.findings
                .map { it.id }
                .toSet()
                .size != snapshot.findings.size
        ) {
            return false
        }

        if (snapshot.traffic.any { item ->
                !isValidUuidToken(item.serviceUuid) ||
                    !isValidUuidToken(item.characteristicUuid) ||
                    item.notificationCount <= 0 ||
                    item.lastSeenAtMillis !in 0L..MAX_TIMESTAMP ||
                    item.lastPayloadBytes !in 0..MAX_PAYLOAD_BYTES ||
                    item.lastFrameCommand?.let { it !in 0..255 } == true ||
                    item.lastFrameResponse?.let { it !in 0..255 } == true ||
                    item.batteryCandidatePercent?.let { it !in 0..100 } == true
            }
        ) {
            return false
        }
        val trafficKeys =
            snapshot.traffic.map {
                it.serviceUuid.lowercase() + "/" + it.characteristicUuid.lowercase()
            }
        if (trafficKeys.toSet().size != trafficKeys.size) return false
        return true
    }

    private fun isValidUuidToken(value: String): Boolean =
        value.isNotBlank() && value.length <= 64 &&
            value.none { it.isWhitespace() || Character.isISOControl(it) }
}
