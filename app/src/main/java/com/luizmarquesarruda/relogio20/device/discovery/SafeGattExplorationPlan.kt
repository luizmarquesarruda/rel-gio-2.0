package com.luizmarquesarruda.relogio20.device.discovery

/**
 * Builds a deterministic, read-only exploration plan from an already discovered
 * GATT inventory. Proprietary write channels and firmware-update services are
 * never eligible. Reads are restricted to Bluetooth SIG characteristics whose
 * semantics and payload formats are standardized.
 */
internal object SafeGattExplorationPlan {
    data class Operation(
        val serviceUuid: String,
        val characteristicUuid: String,
        val kind: Kind,
        val reason: String
    )

    enum class Kind { READ_STANDARD_VALUE, SUBSCRIBE_STANDARD_MEASUREMENT }

    private data class KnownCharacteristic(
        val service: String,
        val characteristic: String,
        val read: Boolean,
        val notify: Boolean,
        val description: String
    )

    private val known = listOf(
        KnownCharacteristic("180a", "2a29", true, false, "fabricante"),
        KnownCharacteristic("180a", "2a24", true, false, "modelo"),
        KnownCharacteristic("180a", "2a25", true, false, "número de série"),
        KnownCharacteristic("180a", "2a26", true, false, "versão de firmware"),
        KnownCharacteristic("180a", "2a27", true, false, "versão de hardware"),
        KnownCharacteristic("180a", "2a28", true, false, "versão de software"),
        KnownCharacteristic("180f", "2a19", true, true, "bateria"),
        KnownCharacteristic("1805", "2a2b", true, false, "hora atual"),
        KnownCharacteristic("1809", "2a1c", true, true, "temperatura"),
        KnownCharacteristic("180d", "2a37", false, true, "frequência cardíaca"),
        KnownCharacteristic("1822", "2a5f", false, true, "oximetria")
    )

    fun build(services: List<GattServiceRecord>): List<Operation> {
        val plan = mutableListOf<Operation>()
        services.forEach { service ->
            val serviceShort = shortUuid(service.uuid)
            if (serviceShort == "fe59") return@forEach // Nordic Secure DFU: never explore.
            service.characteristics.forEach { characteristic ->
                val characteristicShort = shortUuid(characteristic.uuid)
                val profile = known.firstOrNull {
                    it.service == serviceShort && it.characteristic == characteristicShort
                } ?: return@forEach
                val properties = characteristic.properties
                if (profile.read && "READ" in properties) {
                    plan += Operation(
                        service.uuid, characteristic.uuid, Kind.READ_STANDARD_VALUE,
                        "Leitura SIG padronizada: ${profile.description}."
                    )
                }
                if (profile.notify && ("NOTIFY" in properties || "INDICATE" in properties) &&
                    characteristic.descriptorUuids.any { shortUuid(it) == "2902" }) {
                    plan += Operation(
                        service.uuid, characteristic.uuid, Kind.SUBSCRIBE_STANDARD_MEASUREMENT,
                        "Notificação SIG padronizada: ${profile.description}."
                    )
                }
            }
        }
        return plan.distinctBy { it.serviceUuid.lowercase() + "/" + it.characteristicUuid.lowercase() + "/" + it.kind }
    }

    private fun shortUuid(value: String): String {
        val normalized = value.trim().lowercase()
        return when {
            normalized.length == 4 -> normalized
            normalized.startsWith("0000") && normalized.length >= 8 -> normalized.substring(4, 8)
            else -> normalized
        }
    }
}
