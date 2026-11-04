package com.luizmarquesarruda.relogio20.device.discovery

/**
 * Human-guided experiments correlate observable actions with passive BLE traffic.
 * No proprietary command is sent and no sensor value is inferred from timing alone.
 */
internal object WatchExperimentCatalog {
    data class Experiment(
        val id: String,
        val title: String,
        val instruction: String,
        val expectedEvidence: String
    )

    val all = listOf(
        Experiment("battery", "Bateria", "Registre o percentual exibido no relógio e compare após nova captura.", "Mudança de payload correlacionada a pelo menos três níveis de bateria."),
        Experiment("clock", "Hora e data", "Compare o relógio com a hora do Android, incluindo fuso horário e formato 12/24 h.", "Campo de data/hora decodificado e confirmado após reinicialização ou reconexão."),
        Experiment("temperature", "Temperatura", "Verifique se o valor aparece no relógio como temperatura corporal ou clima; registre unidade e tela.", "Origem e unidade identificadas; comparar com referência independente adequada."),
        Experiment("heart_rate", "Frequência cardíaca", "Inicie uma medição manual no relógio e aguarde a conclusão sem enviar comandos BLE.", "Valor recebido durante a janela da medição e comparado com leitura independente."),
        Experiment("spo2", "Oxigenação", "Inicie a medição manual de SpO2 e registre o horário exato.", "Campo e unidade identificados, com repetição e referência independente."),
        Experiment("steps", "Passos", "Anote o contador antes e depois de uma caminhada curta controlada.", "Incremento BLE distinto de dados agregados do Health Connect."),
        Experiment("sleep", "Sono", "Compare os registros após uma noite e verifique se o relógio exporta histórico pelo app.", "Estrutura de estágios/duração identificada e origem comprovada."),
        Experiment("activity", "Atividade", "Inicie e encerre uma caminhada no relógio, registrando os horários.", "Eventos de início/fim e métricas associados de forma reproduzível.")
    )
}
