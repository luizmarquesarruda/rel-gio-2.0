package com.luizmarquesarruda.relogio20.device.discovery

import java.security.MessageDigest
import java.util.Locale

/**
 * Stable structural signature of a GATT inventory.
 *
 * It includes service/characteristic/descriptor UUIDs and characteristic
 * properties only. It deliberately excludes MAC addresses, serial numbers,
 * manufacturer values and all measurement payloads. This is not device
 * authentication and is not a hash of the firmware binary.
 */
internal object GattProfileSignature {
    data class Signature(
        val sha256: String,
        val serviceCount: Int,
        val characteristicCount: Int
    )

    enum class Change {
        FIRST_CAPTURE,
        UNCHANGED,
        CHANGED
    }

    fun calculate(services: List<GattServiceRecord>): Signature {
        val canonicalServices = services.map { service ->
            val characteristics = service.characteristics.map { characteristic ->
                val properties = characteristic.properties
                    .map { it.trim().uppercase(Locale.ROOT) }
                    .filter { it.isNotEmpty() }
                    .sorted()
                    .joinToString(",")
                val descriptors = characteristic.descriptorUuids
                    .map { normalizeUuid(it) }
                    .sorted()
                    .joinToString(",")
                normalizeUuid(characteristic.uuid) + "|p=" + properties + "|d=" + descriptors
            }.sorted()
            normalizeUuid(service.uuid) + "{" + characteristics.joinToString(";") + "}"
        }.sorted()

        val canonical = canonicalServices.joinToString("\n")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }

        return Signature(
            sha256 = digest,
            serviceCount = services.size,
            characteristicCount = services.sumOf { it.characteristics.size }
        )
    }

    fun compare(current: Signature, previousSha256: String?): Change {
        val previous = previousSha256
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.matches(Regex("^[0-9a-f]{64}$")) }
            ?: return Change.FIRST_CAPTURE
        return if (current.sha256.equals(previous, ignoreCase = true)) {
            Change.UNCHANGED
        } else {
            Change.CHANGED
        }
    }

    private fun normalizeUuid(value: String): String =
        value.trim().lowercase(Locale.ROOT)
}
