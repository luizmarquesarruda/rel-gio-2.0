package com.luizmarquesarruda.relogio20.device.discovery

/**
 * Describes only what the current GATT inventory exposes.
 *
 * A missing SIG profile does not prove that the physical sensor is absent: the
 * manufacturer may expose it through a proprietary channel or only after an
 * undocumented interaction. No operation is initiated by this matrix.
 */
internal object GattCapabilityMatrix {
    enum class State(val label: String) {
        NOT_ADVERTISED("NÃO ANUNCIADO VIA GATT NESTA SESSÃO"),
        SERVICE_WITHOUT_CHARACTERISTIC("SERVIÇO PRESENTE • CARACTERÍSTICA ESPERADA AUSENTE"),
        CHARACTERISTIC_WITHOUT_ACCESS("CARACTERÍSTICA PRESENTE • SEM READ/NOTIFY PADRÃO"),
        READ_AVAILABLE("LEITURA READ ANUNCIADA"),
        NOTIFICATION_AVAILABLE("NOTIFY/INDICATE COM CCCD ANUNCIADO"),
        READ_AND_NOTIFICATION_AVAILABLE("READ + NOTIFY/INDICATE COM CCCD"),
        NOTIFICATION_WITHOUT_CCCD("NOTIFY/INDICATE SEM CCCD PADRÃO"),
        HISTORICAL_TRAFFIC("TRÁFEGO HISTÓRICO REGISTRADO • DADO NÃO VALIDADO"),
        TRAFFIC_OBSERVED("TRÁFEGO RECEBIDO • DADO NÃO VALIDADO")
    }

    data class Assessment(
        val id: String,
        val title: String,
        val state: State,
        val detail: String,
        val evidence: String,
        val observationCount: Int
    )

    private data class Definition(
        val id: String,
        val title: String,
        val service: String,
        val characteristics: Set<String>
    )

    private val definitions = listOf(
        Definition("battery", "Bateria padrão do relógio", "180f", setOf("2a19")),
        Definition(
            "device_information",
            "Identificação padrão do fabricante e firmware",
            "180a",
            setOf("2a29", "2a24", "2a26", "2a27", "2a28")
        ),
        Definition("clock", "Hora atual padronizada (CTS)", "1805", setOf("2a2b")),
        Definition("heart_rate", "Frequência cardíaca padronizada", "180d", setOf("2a37", "2a38")),
        Definition("blood_pressure", "Pressão arterial padronizada", "1810", setOf("2a35", "2a36", "2a49")),
        Definition("pulse_oximeter", "Oximetria padronizada (SpO₂)", "1822", setOf("2a5e", "2a5f", "2a60")),
        Definition("thermometer", "Termômetro padronizado", "1809", setOf("2a1c", "2a1e")),
        Definition("environment", "Sensores ambientais padronizados", "181a", setOf("2a6d", "2a6e", "2a6f")),
        Definition("body_composition", "Composição corporal padronizada", "181b", setOf("2a9b", "2a9c")),
        Definition("running_cadence", "Velocidade e cadência de corrida", "1814", setOf("2a53", "2a54")),
        Definition("cycling_cadence", "Velocidade e cadência de ciclismo", "1816", setOf("2a5b", "2a5c"))
    )

    fun evaluate(
        services: List<GattServiceRecord>,
        traffic: List<CharacteristicTraffic> = emptyList()
    ): List<Assessment> {
        val groupedServices = services.groupBy { shortUuid(it.uuid) }
        return definitions.map { definition ->
            val matchingServices = groupedServices[definition.service].orEmpty()
            val servicePresent = matchingServices.isNotEmpty()
            val matchingCharacteristics = matchingServices.flatMap { service ->
                service.characteristics.filter { shortUuid(it.uuid) in definition.characteristics }
            }
            val readable = matchingCharacteristics.filter { "READ" in normalizedProperties(it.properties) }
            val notifyProperties = matchingCharacteristics.filter {
                val properties = normalizedProperties(it.properties)
                "NOTIFY" in properties || "INDICATE" in properties
            }
            val notifiable = notifyProperties.filter { characteristic ->
                characteristic.descriptorUuids.any { shortUuid(it) == "2902" }
            }
            val notifyWithoutCccd = notifyProperties.size - notifiable.size
            val trafficCount = matchingCharacteristics.sumOf { characteristic ->
                traffic.filter { item ->
                    shortUuid(item.serviceUuid) == definition.service &&
                        shortUuid(item.characteristicUuid) == shortUuid(characteristic.uuid)
                }.sumOf { it.notificationCount }
            }

            val state = when {
                !servicePresent -> State.NOT_ADVERTISED
                matchingCharacteristics.isEmpty() -> State.SERVICE_WITHOUT_CHARACTERISTIC
                trafficCount > 0 -> State.HISTORICAL_TRAFFIC
                readable.isNotEmpty() && notifiable.isNotEmpty() -> State.READ_AND_NOTIFICATION_AVAILABLE
                readable.isNotEmpty() && notifyProperties.isNotEmpty() ->
                    State.READ_AND_NOTIFICATION_WITHOUT_CCCD
                readable.isNotEmpty() -> State.READ_AVAILABLE
                notifiable.isNotEmpty() -> State.NOTIFICATION_AVAILABLE
                notifyProperties.isNotEmpty() -> State.NOTIFICATION_WITHOUT_CCCD
                else -> State.CHARACTERISTIC_WITHOUT_ACCESS
            }

            val characteristicDetails = matchingCharacteristics.map { characteristic ->
                val properties = normalizedProperties(characteristic.properties).sorted()
                val propertyText = properties.joinToString(",").ifEmpty { "sem propriedades" }
                val hasCccd = characteristic.descriptorUuids.any { shortUuid(it) == "2902" }
                val cccdText = if (
                    ("NOTIFY" in properties || "INDICATE" in properties) && !hasCccd
                ) "; sem CCCD" else ""
                shortUuid(characteristic.uuid) + " [" + propertyText + cccdText + "]"
            }
            val notificationNote = if (notifyWithoutCccd > 0) {
                " NOTIFY/INDICATE sem CCCD não é elegível para inscrição padrão."
            } else {
                ""
            }
            val trafficNote = if (trafficCount > 0) {
                " Há " + trafficCount + " notificações no histórico; sem validação semântica."
            } else {
                " Presença/propriedade GATT não comprova que o sensor produza medições."
            }
            val detail = when {
                !servicePresent ->
                    "O serviço SIG " + definition.service + " não foi anunciado neste inventário. " +
                        "Isso não prova ausência física da função: ela pode usar canal proprietário."
                matchingCharacteristics.isEmpty() ->
                    "O serviço SIG " + definition.service + " foi encontrado, mas nenhuma característica " +
                        "esperada para este recurso apareceu."
                else ->
                    "Características: " + characteristicDetails.joinToString("; ") + "." +
                        notificationNote + trafficNote
            }
            val evidence = if (matchingCharacteristics.isEmpty()) {
                "Inventário GATT da sessão; serviço=" + definition.service + "."
            } else {
                "Serviço=" + definition.service + "; características=" +
                    matchingCharacteristics.joinToString(",") { shortUuid(it.uuid) } +
                    "; propriedades e CCCD observados."
            }

            Assessment(
                id = definition.id,
                title = definition.title,
                state = state,
                detail = detail,
                evidence = evidence,
                observationCount = trafficCount
            )
        }
    }

    fun featureIdFor(serviceUuid: String, characteristicUuid: String): String? {
        val service = shortUuid(serviceUuid)
        val characteristic = shortUuid(characteristicUuid)
        return definitions.firstOrNull {
            it.service == service && characteristic in it.characteristics
        }?.id
    }

    private fun normalizedProperties(properties: Set<String>): Set<String> =
        properties.map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()

    private fun shortUuid(value: String): String {
        val normalized = value.trim().lowercase()
        if (normalized.length == 4) return normalized
        val sigBase = Regex("^0000([0-9a-f]{4})-0000-1000-8000-00805f9b34fb$")
        return sigBase.matchEntire(normalized)?.groupValues?.get(1) ?: normalized
    }
}
