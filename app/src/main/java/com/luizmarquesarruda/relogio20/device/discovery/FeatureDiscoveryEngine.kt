package com.luizmarquesarruda.relogio20.device.discovery

internal data class GattCharacteristicRecord(
    val uuid: String,
    val properties: Set<String>,
    val descriptorUuids: Set<String> = emptySet()
)

internal data class GattServiceRecord(
    val uuid: String,
    val characteristics: List<GattCharacteristicRecord>
)

internal data class FeatureFinding(
    val id: String,
    val category: String,
    val title: String,
    val status: String,
    val detail: String,
    val evidence: String,
    val observationCount: Int = 0
)

internal data class CharacteristicTraffic(
    val serviceUuid: String,
    val characteristicUuid: String,
    val notificationCount: Int,
    val lastSeenAtMillis: Long,
    val lastPayloadBytes: Int,
    val lastFrameCommand: Int? = null,
    val lastFrameResponse: Int? = null,
    val batteryCandidatePercent: Int? = null
)

internal data class DiscoverySnapshot(
    val schemaVersion: Int = 1,
    val updatedAtMillis: Long,
    val serviceCount: Int,
    val characteristicCount: Int,
    val findings: List<FeatureFinding>,
    val traffic: List<CharacteristicTraffic>
)

/**
 * Passive GATT feature discovery.
 *
 * A UUID/property match proves only that a GATT profile or channel is present.
 * It does not prove that the watch implements the user-facing feature, that
 * values are valid, or that a proprietary payload has a known meaning.
 */
internal object FeatureDiscoveryEngine {
    private data class StandardProfile(
        val serviceUuid: String,
        val characteristicUuid: String?,
        val category: String,
        val title: String
    )

    private val standardProfiles = listOf(
        StandardProfile("180d", "2a37", "Saúde", "Perfil padrão de frequência cardíaca"),
        StandardProfile("180f", "2a19", "Dispositivo", "Perfil padrão de bateria"),
        StandardProfile("1805", "2a2b", "Dispositivo", "Sincronização de hora Bluetooth (Current Time Service)"),
        StandardProfile("1822", "2a5f", "Saúde", "Perfil padrão de oximetria de pulso"),
        StandardProfile("181b", "2a9c", "Saúde", "Perfil padrão de composição corporal"),
        StandardProfile("1814", "2a53", "Atividade", "Perfil padrão de velocidade e cadência de corrida"),
        StandardProfile("1816", "2a5b", "Atividade", "Perfil padrão de velocidade e cadência de ciclismo"),
        StandardProfile("1809", "2a1c", "Saúde", "Perfil padrão de termômetro"),
        StandardProfile("180a", null, "Dispositivo", "Serviço padrão de informações do dispositivo")
    )

    private val standardServiceIds = standardProfiles.map { it.serviceUuid }.toSet()
    private const val NUS_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
    private const val NUS_RX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
    private const val NUS_TX = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"

    fun captureInventory(
        services: List<GattServiceRecord>,
        capturedAtMillis: Long,
        previous: DiscoverySnapshot? = null
    ): DiscoverySnapshot {
        val traffic = previous?.traffic.orEmpty()
        val trafficByKey = traffic.associateBy { trafficKey(it.serviceUuid, it.characteristicUuid) }
        val findings = mutableListOf<FeatureFinding>()
        val normalizedServices = services.associateBy { shortUuid(it.uuid) }
        val serviceCount = services.size
        val characteristicCount = services.sumOf { it.characteristics.size }

        standardProfiles.forEach { profile ->
            val service = normalizedServices[profile.serviceUuid] ?: return@forEach
            val matchingCharacteristics = if (profile.characteristicUuid == null) {
                service.characteristics
            } else {
                service.characteristics.filter {
                    shortUuid(it.uuid) == profile.characteristicUuid
                }
            }
            if (matchingCharacteristics.isNotEmpty()) {
                val props = matchingCharacteristics.flatMap { it.properties }.toSet()
                val channel = when {
                    profile.serviceUuid == "1805" && "WRITE" in props ->
                        "característica Current Time com WRITE anunciada; sincronização padrão habilitável"
                    profile.serviceUuid == "1805" ->
                        "característica Current Time encontrada sem WRITE; ajuste não habilitável"
                    "NOTIFY" in props || "INDICATE" in props -> "canal de notificação anunciado"
                    "READ" in props -> "característica de leitura anunciada"
                    else -> "serviço anunciado"
                }
                findings.add(
                    FeatureFinding(
                        id = "standard:" + profile.serviceUuid,
                        category = profile.category,
                        title = profile.title,
                        status = "PERFIL GATT ENCONTRADO • DADOS NÃO VALIDADOS",
                        detail = "Foi localizado o UUID Bluetooth padrão " +
                            profile.serviceUuid.uppercase() + " / " +
                            (profile.characteristicUuid ?: "serviço") + ". Propriedade observada: " +
                            channel + ". Nenhuma leitura ou inscrição adicional foi iniciada.",
                        evidence = "Correspondência de UUID e inventário GATT."
                    )
                )
            }
        }

        val nus = services.firstOrNull { normalizeUuid(it.uuid) == NUS_SERVICE }
        if (nus != null) {
            val tx = nus.characteristics.firstOrNull { normalizeUuid(it.uuid) == NUS_TX }
            val rx = nus.characteristics.firstOrNull { normalizeUuid(it.uuid) == NUS_RX }
            if (tx != null) {
                val observed = trafficByKey[trafficKey(nus.uuid, tx.uuid)]
                findings.add(
                    FeatureFinding(
                        id = "transport:nus-tx",
                        category = "Conectividade",
                        title = "Canal de notificações NUS TX",
                        status = if ((observed?.notificationCount ?: 0) > 0) {
                            "TRÁFEGO RECEBIDO • SEM SEMÂNTICA TOTALMENTE VALIDADA"
                        } else {
                            "CANAL ENCONTRADO • AGUARDANDO NOTIFICAÇÃO"
                        },
                        detail = "Característica Nordic UART TX localizada. O aplicativo só observa " +
                            "notificações quando a inscrição permitida pelo perfil atual está ativa.",
                        evidence = "UUID NUS TX e propriedades GATT.",
                        observationCount = observed?.notificationCount ?: 0
                    )
                )
            }
            if (rx != null) {
                findings.add(
                    FeatureFinding(
                        id = "transport:nus-rx",
                        category = "Segurança",
                        title = "Canal de entrada NUS RX",
                        status = "ESCRITA BLOQUEADA",
                        detail = "A característica de entrada foi identificada, mas o aplicativo não envia " +
                            "payloads, sondagens ou comandos por este canal.",
                        evidence = "UUID NUS RX e propriedades GATT."
                    )
                )
            }
        }

        services.forEach { service ->
            val serviceShort = shortUuid(service.uuid)
            if (serviceShort in standardServiceIds || normalizeUuid(service.uuid) == NUS_SERVICE) {
                return@forEach
            }
            service.characteristics.forEach { characteristic ->
                val props = characteristic.properties
                val key = trafficKey(service.uuid, characteristic.uuid)
                val observed = trafficByKey[key]
                val canNotify = "NOTIFY" in props || "INDICATE" in props
                val canWrite = "WRITE" in props || "WRITE_NO_RESPONSE" in props

                if (canNotify) {
                    findings.add(
                        FeatureFinding(
                            id = "channel:" + key,
                            category = "Canal proprietário",
                            title = "Canal de notificação sem protocolo identificado",
                            status = if ((observed?.notificationCount ?: 0) > 0) {
                                "TRÁFEGO OBSERVADO • CONTEÚDO NÃO DECODIFICADO"
                            } else {
                                "CANAL LOCALIZADO • INSCRIÇÃO NÃO ATIVADA"
                            },
                            detail = "Serviço " + service.uuid + ", característica " +
                                characteristic.uuid + ". O canal não foi inscrito automaticamente.",
                            evidence = "Propriedade NOTIFY/INDICATE no inventário GATT.",
                            observationCount = observed?.notificationCount ?: 0
                        )
                    )
                }
                if (canWrite) {
                    findings.add(
                        FeatureFinding(
                            id = "blocked-write:" + key,
                            category = "Segurança",
                            title = "Canal proprietário de escrita",
                            status = "BLOQUEADO • NENHUM COMANDO ENVIADO",
                            detail = "Serviço " + service.uuid + ", característica " +
                                characteristic.uuid + ". Propriedades de escrita são registradas " +
                                "somente para análise; não são testadas.",
                            evidence = "Propriedade WRITE/WRITE_NO_RESPONSE no inventário GATT."
                        )
                    )
                }
                if (!canNotify && !canWrite && "READ" in props) {
                    findings.add(
                        FeatureFinding(
                            id = "read-only:" + key,
                            category = "Canal proprietário",
                            title = "Característica somente leitura",
                            status = "LOCALIZADA • LEITURA NÃO EXECUTADA",
                            detail = "O UUID foi catalogado sem solicitar leitura do valor.",
                            evidence = "Propriedade READ no inventário GATT."
                        )
                    )
                }
            }
        }

        // Keep protocol observations from earlier sessions; a new GATT inventory
        // must not erase the evidence collected from previous connections.
        previous?.findings.orEmpty()
            .filter { it.id.startsWith("traffic:") || it.id.startsWith("ab-frame:") || it.id == "candidate:battery" }
            .forEach { old ->
                if (findings.none { it.id == old.id }) findings.add(old)
            }

        if (findings.isEmpty()) {
            findings.add(
                FeatureFinding(
                    id = "inventory:unknown",
                    category = "Descoberta",
                    title = "Nenhum perfil conhecido identificado",
                    status = "INVENTÁRIO REGISTRADO",
                    detail = "Os serviços foram inventariados, mas não correspondem aos perfis atualmente catalogados.",
                    evidence = "Inventário GATT passivo."
                )
            )
        }

        return DiscoverySnapshot(
            updatedAtMillis = capturedAtMillis,
            serviceCount = serviceCount,
            characteristicCount = characteristicCount,
            findings = findings.distinctBy { it.id }.take(160),
            traffic = traffic.take(128)
        )
    }

    fun observeNotification(
        previous: DiscoverySnapshot,
        serviceUuid: String,
        characteristicUuid: String,
        payloadBytes: Int,
        observedAtMillis: Long,
        frameCommand: Int? = null,
        frameResponse: Int? = null,
        batteryCandidatePercent: Int? = null
    ): DiscoverySnapshot {
        if (payloadBytes < 0) return previous
        val key = trafficKey(serviceUuid, characteristicUuid)
        val old = previous.traffic.firstOrNull {
            trafficKey(it.serviceUuid, it.characteristicUuid) == key
        }
        val current = CharacteristicTraffic(
            serviceUuid = serviceUuid,
            characteristicUuid = characteristicUuid,
            notificationCount = (old?.notificationCount ?: 0) + 1,
            lastSeenAtMillis = observedAtMillis,
            lastPayloadBytes = payloadBytes,
            lastFrameCommand = frameCommand,
            lastFrameResponse = frameResponse,
            batteryCandidatePercent = batteryCandidatePercent
        )
        val traffic = (previous.traffic.filter {
            trafficKey(it.serviceUuid, it.characteristicUuid) != key
        } + current).takeLast(128)

        val findings = previous.findings.toMutableList()
        upsert(
            findings,
            FeatureFinding(
                id = "traffic:" + key,
                category = "Observação BLE",
                title = "Notificações recebidas",
                status = "TRÁFEGO OBSERVADO • SEM INTERPRETAÇÃO AUTOMÁTICA",
                detail = "Foram recebidas " + current.notificationCount +
                    " notificações neste canal. Último pacote: " + payloadBytes +
                    " bytes. O conteúdo bruto não foi persistido.",
                evidence = "Callback de notificação GATT.",
                observationCount = current.notificationCount
            )
        )

        val previousChannelFinding = findings.firstOrNull { it.id == "channel:" + key }
        if (previousChannelFinding != null) {
            upsert(
                findings,
                previousChannelFinding.copy(
                    status = "TRÁFEGO OBSERVADO • CONTEÚDO NÃO DECODIFICADO",
                    detail = "Serviço " + serviceUuid + ", característica " + characteristicUuid +
                        ". Foram observadas " + current.notificationCount +
                        " notificações; o canal continua sem semântica confirmada.",
                    observationCount = current.notificationCount
                )
            )
        }
        if (normalizeUuid(serviceUuid) == NUS_SERVICE && normalizeUuid(characteristicUuid) == NUS_TX) {
            upsert(
                findings,
                FeatureFinding(
                    id = "transport:nus-tx",
                    category = "Conectividade",
                    title = "Canal de notificações NUS TX",
                    status = "TRÁFEGO RECEBIDO • SEM SEMÂNTICA TOTALMENTE VALIDADA",
                    detail = "Foram recebidas " + current.notificationCount +
                        " notificações pelo canal NUS TX. O conteúdo é classificado somente quando " +
                        "corresponde a um formato conhecido e ainda sujeito a validação.",
                    evidence = "UUID NUS TX e callback de notificação GATT.",
                    observationCount = current.notificationCount
                )
            )
        }
        val standardService = shortUuid(serviceUuid)
        val standardCharacteristic = shortUuid(characteristicUuid)
        val standardProfile = standardProfiles.firstOrNull {
            it.serviceUuid == standardService &&
                (it.characteristicUuid == null || it.characteristicUuid == standardCharacteristic)
        }
        if (standardProfile != null) {
            val existing = findings.firstOrNull { it.id == "standard:" + standardProfile.serviceUuid }
            if (existing != null) {
                upsert(
                    findings,
                    existing.copy(
                        status = "TRÁFEGO OBSERVADO • VALOR NÃO VALIDADO",
                        detail = "Notificação recebida no perfil GATT padrão " +
                            standardProfile.serviceUuid.uppercase() +
                            ". O valor não foi decodificado nem validado como medição."
                    )
                )
            }
        }

        if (frameCommand != null && frameResponse != null) {
            val commandHex = frameCommand.toString(16).padStart(2, '0').uppercase()
            val responseHex = frameResponse.toString(16).padStart(2, '0').uppercase()
            val isBatteryCandidate = batteryCandidatePercent != null
            upsert(
                findings,
                FeatureFinding(
                    id = "ab-frame:" + commandHex + ":" + responseHex,
                    category = "Protocolo candidato",
                    title = "Quadro AB " + commandHex + "/" + responseHex,
                    status = if (isBatteryCandidate) {
                        "INTERPRETAÇÃO CANDIDATA • NÃO VALIDADA"
                    } else {
                        "FORMATO OBSERVADO • SEM SEMÂNTICA CONFIRMADA"
                    },
                    detail = if (isBatteryCandidate) {
                        "O padrão coincide com a hipótese de bateria do perfil AB. É necessário " +
                            "comparar com Wearfit Pro em diferentes níveis de carga."
                    } else {
                        "Cabeçalho e campos de comando/resposta foram reconhecidos, mas a finalidade " +
                            "deste quadro permanece desconhecida."
                    },
                    evidence = "Parser AB local; nenhum comando foi enviado.",
                    observationCount = current.notificationCount
                )
            )
        }

        if (batteryCandidatePercent != null && batteryCandidatePercent in 0..100) {
            upsert(
                findings,
                FeatureFinding(
                    id = "candidate:battery",
                    category = "Dispositivo",
                    title = "Bateria do relógio (hipótese)",
                    status = "CANDIDATO • NÃO VALIDADO",
                    detail = "Último valor interpretado: " + batteryCandidatePercent +
                        "%. Não publicar em Health Connect nem tratar como medição confirmada " +
                        "até a correlação com Wearfit Pro.",
                    evidence = "Quadro AB 0x91/0x80 recebido pelo NUS TX.",
                    observationCount = current.notificationCount
                )
            )
        }

        return previous.copy(
            updatedAtMillis = observedAtMillis,
            findings = findings.distinctBy { it.id }.take(160),
            traffic = traffic
        )
    }

    fun emptySnapshot(nowMillis: Long): DiscoverySnapshot = DiscoverySnapshot(
        updatedAtMillis = nowMillis,
        serviceCount = 0,
        characteristicCount = 0,
        findings = emptyList(),
        traffic = emptyList()
    )

    private fun upsert(items: MutableList<FeatureFinding>, finding: FeatureFinding) {
        val index = items.indexOfFirst { it.id == finding.id }
        if (index >= 0) items[index] = finding else items.add(finding)
    }

    private fun trafficKey(serviceUuid: String, characteristicUuid: String): String =
        normalizeUuid(serviceUuid) + "/" + normalizeUuid(characteristicUuid)

    private fun normalizeUuid(value: String): String = value.trim().lowercase()

    private fun shortUuid(value: String): String {
        val normalized = normalizeUuid(value)
        return when {
            normalized.length == 4 -> normalized
            normalized.startsWith("0000") && normalized.length >= 8 -> normalized.substring(4, 8)
            else -> normalized
        }
    }
}
