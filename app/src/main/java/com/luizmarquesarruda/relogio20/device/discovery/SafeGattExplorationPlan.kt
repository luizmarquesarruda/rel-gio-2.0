package com.luizmarquesarruda.relogio20.device.discovery

/**
 * Deterministic GATT plans. The automatic plan is restricted to known Bluetooth SIG
 * characteristics. The deeper plan is manual and may READ or subscribe to custom
 * channels, but it never writes characteristic payloads or touches DFU/NUS RX.
 */
internal object SafeGattExplorationPlan {
    data class Operation(
        val serviceUuid: String,
        val characteristicUuid: String,
        val kind: Kind,
        val reason: String
    )

    enum class Kind {
        READ_STANDARD_VALUE,
        SUBSCRIBE_STANDARD_MEASUREMENT,
        READ_PROPRIETARY_VALUE,
        SUBSCRIBE_PROPRIETARY_CHANNEL
    }

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
        KnownCharacteristic("180a", "2a26", true, false, "versão de firmware"),
        KnownCharacteristic("180a", "2a27", true, false, "versão de hardware"),
        KnownCharacteristic("180a", "2a28", true, false, "versão de software"),
        KnownCharacteristic("180f", "2a19", true, true, "bateria"),
        KnownCharacteristic("1805", "2a2b", true, false, "hora atual"),
        KnownCharacteristic("1809", "2a1c", false, true, "temperatura medida"),
        KnownCharacteristic("1809", "2a1e", false, true, "temperatura intermediária"),
        KnownCharacteristic("180d", "2a37", false, true, "frequência cardíaca"),
        KnownCharacteristic("1810", "2a35", false, true, "pressão arterial"),
        KnownCharacteristic("1810", "2a36", false, true, "pressão intermediária"),
        KnownCharacteristic("1810", "2a49", true, false, "recursos de pressão arterial"),
        KnownCharacteristic("181a", "2a6d", true, true, "pressão ambiental"),
        KnownCharacteristic("181a", "2a6e", true, true, "temperatura ambiente"),
        KnownCharacteristic("181a", "2a6f", true, true, "umidade"),
        KnownCharacteristic("1822", "2a5e", true, false, "oximetria spot-check"),
        KnownCharacteristic("1822", "2a5f", false, true, "oximetria contínua"),
        KnownCharacteristic("1822", "2a60", true, false, "recursos de oximetria"),
        KnownCharacteristic("181b", "2a9c", false, true, "composição corporal"),
        KnownCharacteristic("1814", "2a53", false, true, "velocidade/cadência de corrida"),
        KnownCharacteristic("1814", "2a54", true, false, "recursos de corrida"),
        KnownCharacteristic("1816", "2a5b", false, true, "velocidade/cadência de ciclismo"),
        KnownCharacteristic("1816", "2a5c", true, false, "recursos de ciclismo")
    )

    private val standardServiceIds = known.map { it.service }.toSet() + setOf("1800", "1801")
    private const val NUS_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
    private const val NUS_RX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
    private const val NUS_TX = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"
    private const val MAX_DEEP_OPERATIONS = 24

    /** Safe automatic plan: known SIG payloads only. */
    fun build(services: List<GattServiceRecord>): List<Operation> {
        val plan = mutableListOf<Operation>()
        services.forEach { service ->
            val serviceShort = shortUuid(service.uuid)
            if (serviceShort == "fe59") return@forEach
            service.characteristics.forEach characteristicLoop@{ characteristic ->
                val characteristicShort = shortUuid(characteristic.uuid)
                val profile = known.firstOrNull {
                    it.service == serviceShort && it.characteristic == characteristicShort
                } ?: return@characteristicLoop
                val properties = characteristic.properties
                if (profile.read && "READ" in properties) {
                    plan += Operation(
                        service.uuid, characteristic.uuid, Kind.READ_STANDARD_VALUE,
                        "Leitura Bluetooth SIG padronizada: ${profile.description}."
                    )
                }
                if (profile.notify &&
                    ("NOTIFY" in properties || "INDICATE" in properties) &&
                    characteristic.descriptorUuids.any { shortUuid(it) == "2902" }
                ) {
                    plan += Operation(
                        service.uuid, characteristic.uuid, Kind.SUBSCRIBE_STANDARD_MEASUREMENT,
                        "Notificação/indicação Bluetooth SIG: ${profile.description}."
                    )
                }
            }
        }
        return plan.distinctBy {
            it.serviceUuid.lowercase() + "/" + it.characteristicUuid.lowercase() + "/" + it.kind
        }
    }

    /**
     * Explicitly user-triggered deeper plan. Only custom services are inspected.
     * READ is used only when the characteristic advertises READ. Notifications/
     * indications require CCCD 0x2902. No WRITE operation is ever created.
     */
    fun buildDeep(services: List<GattServiceRecord>): List<Operation> {
        val plan = mutableListOf<Operation>()
        services.forEach { service ->
            val serviceShort = shortUuid(service.uuid)
            val normalizedService = service.uuid.trim().lowercase()
            if (serviceShort == "fe59" ||
                serviceShort in standardServiceIds ||
                normalizedService == NUS_SERVICE
            ) return@forEach

            service.characteristics.forEach characteristicLoop@{ characteristic ->
                val characteristicShort = shortUuid(characteristic.uuid)
                if (characteristicShort in setOf("2a25", "2a23", "2a50")) {
                    return@characteristicLoop
                }
                if (normalizedService == NUS_SERVICE &&
                    characteristic.uuid.trim().lowercase() in setOf(NUS_RX, NUS_TX)
                ) return@characteristicLoop

                val properties = characteristic.properties
                if ("READ" in properties) {
                    plan += Operation(
                        service.uuid, characteristic.uuid, Kind.READ_PROPRIETARY_VALUE,
                        "Leitura manual de canal customizado; nenhum payload será escrito."
                    )
                }
                if (("NOTIFY" in properties || "INDICATE" in properties) &&
                    characteristic.descriptorUuids.any { shortUuid(it) == "2902" }
                ) {
                    plan += Operation(
                        service.uuid, characteristic.uuid, Kind.SUBSCRIBE_PROPRIETARY_CHANNEL,
                        "Inscrição manual via CCCD padrão; sem envio de comando proprietário."
                    )
                }
            }
        }
        return plan.distinctBy {
            it.serviceUuid.lowercase() + "/" + it.characteristicUuid.lowercase() + "/" + it.kind
        }.take(MAX_DEEP_OPERATIONS)
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
