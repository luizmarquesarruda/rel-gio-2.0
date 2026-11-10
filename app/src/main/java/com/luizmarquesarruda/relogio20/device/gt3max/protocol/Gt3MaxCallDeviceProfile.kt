package com.luizmarquesarruda.relogio20.device.gt3max.protocol

import java.util.Locale
import java.util.UUID

/**
 * Runtime profile for the owner's GT3 MAX Call.
 *
 * Firmware/build labels are owner-reported metadata, not values read from GATT.
 * The Bluetooth address and serial are intentionally not stored in this public profile.
 */
internal object Gt3MaxCallDeviceProfile {
    const val MODEL_NAME = "GT3 MAX"
    const val VARIANT_NAME = "GT3 MAX Call"
    const val REPORTED_FIRMWARE_VERSION = "V1.07"
    const val REPORTED_SOFTWARE_BUILD = "SH0050-1_1.1.3_1.23_1.18_1.1"

    val NUS_SERVICE_UUID: UUID =
        UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    val NUS_RX_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    val NUS_TX_CHARACTERISTIC_UUID: UUID =
        UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")

    // Captured custom GATT services. They are a fingerprint, not command documentation.
    private val capturedCustomCharacteristics =
        mapOf(
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

    enum class Match {
        CAPTURED_GATT_FINGERPRINT,
        DEVICE_NAME_AND_NUS,
        DEVICE_NAME_ONLY,
        NUS_ONLY,
        OTHER,
    }

    fun isKnownDeviceName(name: String?): Boolean {
        val normalized =
            name
                ?.uppercase(Locale.ROOT)
                ?.filter { it.isLetterOrDigit() }
                ?: return false
        return normalized == "GT3MAX" || normalized == "GT3MAXCALL"
    }

    fun hasNusTransport(services: Map<UUID, Set<UUID>>): Boolean {
        val characteristics = services[NUS_SERVICE_UUID] ?: return false
        return NUS_RX_CHARACTERISTIC_UUID in characteristics &&
            NUS_TX_CHARACTERISTIC_UUID in characteristics
    }

    fun evaluate(
        advertisedName: String?,
        services: Map<UUID, Set<UUID>>,
    ): Match {
        val nameMatches = isKnownDeviceName(advertisedName)
        val nusMatches = hasNusTransport(services)
        val capturedCustomMatches =
            capturedCustomCharacteristics.all { (service, expected) ->
                services[service]?.containsAll(expected) == true
            }

        return when {
            nusMatches && capturedCustomMatches -> {
                Match.CAPTURED_GATT_FINGERPRINT
            }

            nameMatches && nusMatches -> {
                Match.DEVICE_NAME_AND_NUS
            }

            nameMatches -> {
                Match.DEVICE_NAME_ONLY
            }

            nusMatches -> {
                Match.NUS_ONLY
            }

            else -> {
                Match.OTHER
            }
        }
    }

    /**
     * NUS TX notifications use the standard CCCD. The NUS RX write channel remains unused.
     * Full captured custom-service fingerprint + NUS are required before auto-subscribing;
     * the advertised local name may be absent from BLE scan/cache results.
     * This is a compatibility check, not cryptographic device authentication.
     */
    fun shouldAutoSubscribeBattery(
        advertisedName: String?,
        services: Map<UUID, Set<UUID>>,
    ): Boolean = evaluate(advertisedName, services) == Match.CAPTURED_GATT_FINGERPRINT
}
