package com.luizmarquesarruda.relogio20.domain.diagnostics

internal object BleDiagnosticReport {
    fun maskAddress(address: String?): String =
        if (address.isNullOrBlank()) "não disponível" else "identificação ocultada"

    /**
     * Diagnostic TXT is a shareable artifact. It intentionally excludes free-form logs,
     * device identifiers, decoded health values and raw BLE payloads.
     */
    fun build(
        appVersion: String,
        androidVersion: String,
        connectionState: String,
        deviceName: String?,
        deviceAddress: String?,
        generatedAt: String,
        logLines: List<String>,
        discoveryEntries: List<String> = emptyList()
    ): String = buildString {
        appendLine("RELÓGIO 2.0 — RELATÓRIO DE DIAGNÓSTICO BLE")
        appendLine("Gerado em: " + generatedAt)
        appendLine()
        appendLine("[APLICATIVO E AMBIENTE]")
        appendLine("Versão do aplicativo: " + appVersion)
        appendLine("Android: " + androidVersion)
        appendLine("Relatório gerado localmente no aparelho.")
        appendLine()
        appendLine("[DISPOSITIVO]")
        appendLine("Nome observado: identificação ocultada")
        appendLine("Endereço Bluetooth: identificação ocultada")
        appendLine("Estado da conexão: informação omitida por privacidade")
        appendLine()
        appendLine("[EVIDÊNCIAS E LIMITES]")
        appendLine("- O relatório contém somente metadados não sensíveis.")
        appendLine("- Logs livres, identificadores do dispositivo, valores de saúde e payloads BLE foram omitidos.")
        appendLine("- UUIDs e propriedades GATT não identificam, por si só, o protocolo proprietário.")
        appendLine("- Interpretações candidatas ou não validadas não são medições confirmadas.")
        appendLine("- Conexão BLE/GATT não comprova leitura correta de sensores.")
        appendLine()
        appendLine("[REGISTRO DA SESSÃO]")
        appendLine("Eventos técnicos omitidos por privacidade: " + logLines.size)
        appendLine()
        appendLine("[DESCOBERTAS PERSISTIDAS]")
        appendLine("Registros técnicos omitidos por privacidade: " + discoveryEntries.size)
        appendLine()
        appendLine("[CLASSIFICAÇÃO]")
        appendLine("Observado: somente metadados técnicos sem conteúdo de payload.")
        appendLine("Hipótese: qualquer interpretação ainda não comparada com uma referência independente.")
        appendLine("Validado: somente quando houver teste reproduzível documentado; este relatório não promove hipóteses a validações.")
    }
}
