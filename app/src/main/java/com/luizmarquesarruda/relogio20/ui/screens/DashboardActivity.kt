package com.luizmarquesarruda.relogio20.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.luizmarquesarruda.relogio20.data.preferences.AppPreferences
import com.luizmarquesarruda.relogio20.ui.RelogioUi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Main daily-use dashboard. It never infers a watch connection from a previous session. */
class DashboardActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clockCard: android.widget.TextView

    private val clockTick = object : Runnable {
        override fun run() {
            if (!::clockCard.isInitialized) return
            renderClock()
            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val body = RelogioUi.install(
            this,
            "RELÓGIO 2.0",
            "PAINEL PRINCIPAL • SAÚDE & FITNESS",
            showBack = false
        )

        clockCard = RelogioUi.textView(this, "", 16f, RelogioUi.text, true)
        RelogioUi.card(
            body,
            "VISÃO GERAL",
            "Horário do telefone e indicadores principais. Dados sem fonte validada permanecem como N/D.",
            "LOCAL"
        )
        body.addView(clockCard)
        clockCard.setPadding(
            RelogioUi.dp(this, 4),
            RelogioUi.dp(this, 6),
            RelogioUi.dp(this, 4),
            RelogioUi.dp(this, 10)
        )
        renderClock()

        RelogioUi.section(body, "DISPOSITIVO")
        RelogioUi.card(
            body,
            "GT3 Maxx • conexão não consultada",
            "O painel não presume que o relógio esteja conectado. Abra a tela de conexão para iniciar ou verificar o estado BLE/GATT.",
            "VER ESTADO",
            "ABRIR CONEXÃO E DISPOSITIVO"
        ) {
            startActivity(Intent(this, MainActivity::class.java))
        }

        RelogioUi.section(body, "RESUMO")
        RelogioUi.metric(body, "Passos hoje", "N/D", "Consulte a tela Atividade para atualizar a origem Health Connect.")
        RelogioUi.metric(body, "Frequência cardíaca", "N/D", "Sensor direto ainda não validado.")
        RelogioUi.metric(body, "Sono", "N/D", "Dados de sono ainda não disponíveis em fonte validada.")

        RelogioUi.section(body, "NAVEGAÇÃO", "Uma tela principal e sete áreas secundárias independentes")
        RelogioUi.card(body, "01 • Conexão e dispositivo", "Busca BLE, estado do GT3 Maxx, inventário e diagnóstico técnico.", "TELA 1", "ABRIR") {
            startActivity(Intent(this, MainActivity::class.java))
        }
        RelogioUi.card(body, "02 • Saúde", "Frequência cardíaca, SpO₂, pressão e temperatura, com origem e validação explícitas.", "TELA 2", "ABRIR") {
            startActivity(Intent(this, HealthOverviewActivity::class.java))
        }
        RelogioUi.card(body, "03 • Atividade", "Passos agregados do Health Connect; distância, calorias e exercícios somente quando houver fonte válida.", "TELA 3", "ABRIR") {
            startActivity(Intent(this, ActivityOverviewActivity::class.java))
        }
        RelogioUi.card(body, "04 • Sono", "Duração, estágios e tendências; sem preencher lacunas com valores fictícios.", "TELA 4", "ABRIR") {
            startActivity(Intent(this, SleepOverviewActivity::class.java))
        }
        RelogioUi.card(body, "05 • Histórico", "Histórico de passos do Health Connect e acesso separado aos registros técnicos BLE.", "TELA 5", "ABRIR") {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        RelogioUi.card(body, "06 • Mais recursos", "Laboratório BLE, biblioteca de descobertas, catálogo, ferramentas locais e privacidade.", "TELA 6", "ABRIR") {
            startActivity(Intent(this, MoreResourcesActivity::class.java))
        }
        RelogioUi.card(body, "07 • Configurações", "Preferências de horário, unidades e escopo das fontes de saúde.", "TELA 7", "ABRIR") {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        RelogioUi.section(body, "QUALIDADE DOS DADOS")
        RelogioUi.message(body, "Conexão Bluetooth não significa leitura de sensor validada. Valores N/D indicam ausência de dado confiável, não zero. O aplicativo não é um dispositivo médico.")
    }

    override fun onResume() {
        super.onResume()
        if (::clockCard.isInitialized) {
            renderClock()
            handler.removeCallbacks(clockTick)
            handler.post(clockTick)
        }
    }

    override fun onPause() {
        handler.removeCallbacks(clockTick)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockTick)
        super.onDestroy()
    }

    private fun renderClock() {
        val now = LocalDateTime.now()
        val locale = Locale.forLanguageTag("pt-BR")
        val timePattern = if (AppPreferences.uses24HourClock(this)) "HH:mm:ss" else "hh:mm:ss a"
        val time = now.format(DateTimeFormatter.ofPattern(timePattern, locale))
        val date = now.format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", locale))
        clockCard.text = time + "\n" + date.replaceFirstChar { it.titlecase(locale) }
    }
}
