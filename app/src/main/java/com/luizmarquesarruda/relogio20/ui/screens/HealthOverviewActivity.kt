package com.luizmarquesarruda.relogio20.ui.screens

import android.app.Activity
import android.os.Bundle
import com.luizmarquesarruda.relogio20.ui.RelogioUi

/** Health metrics remain unavailable until a source and sample semantics are validated. */
class HealthOverviewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val body = RelogioUi.install(this, "SAÚDE", "MÉTRICAS • ORIGEM • CONFIABILIDADE")
        RelogioUi.section(body, "MEDIÇÕES")
        RelogioUi.metric(body, "Frequência cardíaca", "N/D", "Não há amostra direta do GT3 Maxx validada nesta versão.")
        RelogioUi.metric(body, "Saturação de oxigênio (SpO₂)", "N/D", "Não exibir estimativa como medição confirmada.")
        RelogioUi.metric(body, "Pressão arterial", "N/D", "A medição do relógio não foi validada e não substitui equipamento clínico.")
        RelogioUi.metric(body, "Temperatura", "N/D", "Sensor e unidade ainda não foram confirmados.")
        RelogioUi.section(body, "ORIGEM DOS DADOS")
        RelogioUi.message(
            body,
            "Cada medição futura deverá informar fonte, unidade, horário de recebimento e estado de validação. A conexão BLE, isoladamente, não comprova leitura de saúde.",
        )
        RelogioUi.button(body, "Consultar passos em Atividade") {
            startActivity(android.content.Intent(this, ActivityOverviewActivity::class.java))
        }
    }
}
