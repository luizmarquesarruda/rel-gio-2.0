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

        val timePanel = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(
                RelogioUi.dp(this@DashboardActivity, 16),
                RelogioUi.dp(this@DashboardActivity, 14),
                RelogioUi.dp(this@DashboardActivity, 16),
                RelogioUi.dp(this@DashboardActivity, 14)
            )
            background = RelogioUi.rounded(this@DashboardActivity)
        }
        timePanel.addView(RelogioUi.textView(this, "HORÁRIO DO TELEFONE", 10f, RelogioUi.accent, true))
        clockCard = RelogioUi.textView(this, "", 28f, RelogioUi.text, true).apply {
            setPadding(0, RelogioUi.dp(this@DashboardActivity, 5), 0, 0)
        }
        timePanel.addView(clockCard)
        timePanel.addView(RelogioUi.textView(this, "Fonte local • sistema Android", 10f, RelogioUi.textSubtle))
        body.addView(timePanel, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = RelogioUi.dp(this@DashboardActivity, 10) })
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

        RelogioUi.section(body, "NAVEGAÇÃO", "Uma tela principal e seis áreas secundárias independentes")
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
