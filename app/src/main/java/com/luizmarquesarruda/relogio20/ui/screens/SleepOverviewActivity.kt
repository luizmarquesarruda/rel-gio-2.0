package com.luizmarquesarruda.relogio20.ui.screens

import android.app.Activity
import android.os.Bundle
import com.luizmarquesarruda.relogio20.ui.RelogioUi

/** Sleep screen intentionally contains no synthetic sleep duration or stages. */
class SleepOverviewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val body = RelogioUi.install(this, "SONO", "DURAÇÃO • ESTÁGIOS • TENDÊNCIAS")
        RelogioUi.section(body, "ÚLTIMA NOITE")
        RelogioUi.metric(body, "Duração do sono", "N/D", "Aguardando fonte de sono integrada e validada.")
        RelogioUi.metric(body, "Sono profundo", "N/D", "Estágios ainda não disponíveis.")
        RelogioUi.metric(body, "Sono leve", "N/D", "Estágios ainda não disponíveis.")
        RelogioUi.metric(body, "Sono REM", "N/D", "Estágios ainda não disponíveis.")
        RelogioUi.section(body, "QUALIDADE DA INFORMAÇÃO")
        RelogioUi.message(body, "O GT3 Maxx ainda não forneceu dados de sono com interpretação validada. O aplicativo não inventa duração, estágios ou pontuação.")
    }
}
