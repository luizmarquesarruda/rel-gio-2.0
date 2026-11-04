package com.luizmarquesarruda.relogio20

internal object BleDiagnosticReport {
    private val macPattern = Regex("(?i)(?:[0-9a-f]{2}:){5}[0-9a-f]{2}")

    fun maskAddress(address: String?): String {
        if (address.isNullOrBlank()) return "não disponível"
        val normalized = address.trim()
        if (!macPattern.matches(normalized)) return "identificação ocultada"
        val parts = normalized.split(":")
        return "XX:XX:XX:" + parts.takeLast(3).joinToString(":")
    }

    fun build(
        appVersion: String,
        androidVersion: String,
        connectionState: String,
        deviceName: String?,
        deviceAddress: String?,
        generatedAt: String,
        logLines: List<String>
    ): String {
        val sanitizedLogs = logLines.map { line ->
            macPattern.replace(line) { match -> maskAddress(match.value) }
        }
        return buildString {
            appendLine("RELÓGIO 2.0 — RELATÓRIO DE DIAGNÓSTICO BLE")
            appendLine("Gerado em: " + generatedAt)
            appendLine()
            appendLine("[APLICATIVO E AMBIENTE]")
            appendLine("Versão do aplicativo: " + appVersion)
            appendLine("Android: " + androidVersion)
            appendLine("Relatório gerado localmente no aparelho.")
            appendLine()
            appendLine("[DISPOSITIVO]")
            appendLine("Nome observado: " + (deviceName?.takeIf { it.isNotBlank() } ?: "não disponível"))
            appendLine("Endereço Bluetooth: " + maskAddress(deviceAddress))
            appendLine("Estado da interface no momento da exportação: " + connectionState)
            appendLine()
            appendLine("[EVIDÊNCIAS E LIMITES]")
            appendLine("- Eventos e valores hexadecimais abaixo são observações registradas pelo aplicativo.")
            appendLine("- UUIDs e propriedades GATT observados não identificam, por si só, o protocolo proprietário.")
            appendLine("- Interpretações marcadas como candidatas ou não validadas não são medições confirmadas.")
            appendLine("- Conexão BLE/GATT não comprova leitura correta de sensores.")
            appendLine("- O relatório não inclui conteúdo textual de notificações do telefone.")
            appendLine("- Payloads BLE em hexadecimal podem conter dados de saúde; compartilhe o arquivo somente com quem você autorizou.")
            appendLine()
            appendLine("[REGISTRO DA SESSÃO]")
            if (sanitizedLogs.isEmpty()) {
                appendLine("Nenhum evento registrado nesta sessão.")
            } else {
                sanitizedLogs.forEach(::appendLine)
            }
            appendLine()
            appendLine("[CLASSIFICAÇÃO]")
            appendLine("Observado: eventos, status, UUIDs, propriedades e bytes efetivamente recebidos.")
            appendLine("Hipótese: qualquer interpretação de payload ainda não comparada com uma referência independente.")
            appendLine("Validado: somente quando houver teste reproduzível documentado; este relatório não promove hipóteses a validações.")
        }
    }
}
