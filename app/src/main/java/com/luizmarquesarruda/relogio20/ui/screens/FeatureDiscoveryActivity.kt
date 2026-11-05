package com.luizmarquesarruda.relogio20.ui.screens

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.content.Intent
import com.luizmarquesarruda.relogio20.data.discovery.DiscoveryLibraryKind
import com.luizmarquesarruda.relogio20.data.discovery.DiscoveryLibraryReadResult
import com.luizmarquesarruda.relogio20.data.discovery.FeatureDiscoveryReadResult
import com.luizmarquesarruda.relogio20.data.discovery.FeatureDiscoveryStore
import com.luizmarquesarruda.relogio20.data.discovery.DiscoveryLibraryStore
import com.luizmarquesarruda.relogio20.data.discovery.DiscoveryTestDuration
import com.luizmarquesarruda.relogio20.device.discovery.GattCapabilityMatrix
import com.luizmarquesarruda.relogio20.device.discovery.DiscoverySnapshot
import com.luizmarquesarruda.relogio20.device.discovery.WatchExperimentCatalog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Test workspace: timed passive capture, findings and the local library. */
class FeatureDiscoveryActivity : Activity() {
    private lateinit var body: LinearLayout
    private lateinit var timerValue: TextView
    private lateinit var timerStatus: TextView
    private lateinit var timerCount: TextView
    private lateinit var timerProgress: ProgressBar
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private val store by lazy { DiscoveryLibraryStore(this) }
    private val handler = Handler(Looper.getMainLooper())
    private var selectedDurationSeconds = 120
    private var activeSessionId: String? = null
    private var showExperiments = false

    private val countdownTick = object : Runnable {
        override fun run() {
            val session = store.activeSession(System.currentTimeMillis())
            updateTimerUi(session)
            if (session != null) handler.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::body.isInitialized) {
            render()
            scheduleCountdown()
        }
    }

    override fun onPause() {
        handler.removeCallbacks(countdownTick)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(countdownTick)
        super.onDestroy()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(0xFFF4F6F8.toInt())
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(Button(this).apply {
            text = "‹"
            textSize = 24f
            isAllCaps = false
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(52), dp(48)))
        header.addView(TextView(this).apply {
            text = "Testes e descobertas"
            textSize = 21f
            setTextColor(0xFF14212B.toInt())
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(8), 0, 0, 0)
        })
        root.addView(header)
        root.addView(TextView(this).apply {
            text = "Acompanhe a captura BLE, registre o que o relógio transmite e consulte o histórico local."
            textSize = 12f
            setTextColor(0xFF526574.toInt())
            setPadding(0, dp(6), 0, dp(8))
        })

        val scroll = ScrollView(this).apply { isFillViewport = true }
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(20))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private fun render() {
        body.removeAllViews()
        renderTimerPanel()
        renderAutomaticDiscoveryStatus()
        renderDeepExplorationShortcut()
        renderLibraryShortcut()
        when (val library = store.read()) {
            DiscoveryLibraryReadResult.Empty -> {
                addSectionTitle("Biblioteca interna")
                addMessage("Nenhum registro foi salvo ainda. Descobertas e leituras capturadas aparecerão aqui.")
            }
            is DiscoveryLibraryReadResult.Available -> {
                addSectionTitle("Biblioteca interna")
                addMessage(library.snapshot.records.size.toString() + " registros armazenados localmente.")
            }
            DiscoveryLibraryReadResult.Corrupt -> {
                addSectionTitle("Biblioteca interna")
                addMessage("O arquivo da biblioteca está inválido. Os dados existentes foram preservados e não serão sobrescritos.")
            }
            is DiscoveryLibraryReadResult.UnsupportedSchema -> {
                addSectionTitle("Biblioteca interna")
                addMessage("A biblioteca usa um schema mais novo (" + library.version + "). Os dados foram preservados sem sobrescrita.")
            }
        }
        when (val discovery = FeatureDiscoveryStore(this).read()) {
            FeatureDiscoveryReadResult.Empty -> {
                addSectionTitle("Descobertas GATT")
                addMessage("Conecte o GT3 MAX no painel principal para registrar serviços e características.")
            }
            is FeatureDiscoveryReadResult.Available -> renderSnapshot(discovery.snapshot)
            FeatureDiscoveryReadResult.Corrupt -> {
                addSectionTitle("Descobertas GATT")
                addMessage("O catálogo GATT existente está inválido e foi preservado.")
            }
            is FeatureDiscoveryReadResult.UnsupportedSchema -> {
                addSectionTitle("Descobertas GATT")
                addMessage("O catálogo GATT usa um schema mais novo (" + discovery.version + ") e não será sobrescrito.")
            }
        }
        renderExperimentGuide()
        addSectionTitle("Segurança da exploração")
        addMessage(
            "A captura cronometrada registra somente notificações e leituras recebidas durante a sessão. O HEX capturado permanece no histórico privado (máx. 1.000 registros), com exclusão individual disponível na Biblioteca interna. Nenhum payload proprietário, escrita NUS RX, DFU, OTA, reset ou calibração é enviado."
        )
    }


    private fun renderAutomaticDiscoveryStatus() {
        val enabled = getSharedPreferences("automatic_gatt_discovery", MODE_PRIVATE)
            .getBoolean("enabled", false)
        addSectionTitle("Exploração automática GATT")
        addMessage(
            if (enabled) {
                "Ativa neste aparelho. Ao reconhecer o fingerprint GATT completo do GT3 MAX Call, " +
                    "o app executa leituras READ e inscrições NOTIFY/INDICATE elegíveis sem exigir " +
                    "que você abra este roteiro. A coleta renova janelas de até 10 minutos enquanto " +
                    "o painel principal permanece em primeiro plano e conectado."
            } else {
                "Pausada. No painel principal, ative “Ativar testes automáticos ao conectar” uma vez " +
                    "para que o aplicativo execute e salve os testes GATT automaticamente nas conexões válidas."
            }
        )
    }

    private fun renderDeepExplorationShortcut() {
        val button = Button(this).apply {
            text = "Explorar manualmente GATT"
            isAllCaps = false
            textSize = 14f
            setOnClickListener {
                getSharedPreferences("ble_exploration_requests", MODE_PRIVATE)
                    .edit()
                    .putBoolean("pending_deep_exploration", true)
                    .apply()
                finish()
            }
        }
        body.addView(button, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(50)
        ).apply { bottomMargin = dp(6) })
        addMessage(
            "Esta ação é um atalho manual. O modo automático é controlado no painel principal e não exige esta confirmação a cada conexão."
        )
    }

    private fun renderTimerPanel() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(13), dp(15), dp(13))
            background = roundedSurface()
        }
        addText(card, "TESTE CRONOMETRADO", 10f, 0xFF687786.toInt(), true)
        timerValue = TextView(this).apply {
            text = "02:00"
            textSize = 38f
            setTextColor(0xFF14212B.toInt())
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(4), 0, 0)
            contentDescription = "Tempo restante da sessão de captura"
        }
        card.addView(timerValue)
        timerProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
        }
        card.addView(timerProgress, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(8)
        ).apply { topMargin = dp(5); bottomMargin = dp(7) })
        timerStatus = TextView(this).apply {
            text = "Escolha a duração e inicie a captura local."
            textSize = 12f
            setTextColor(0xFF526574.toInt())
        }
        card.addView(timerStatus)
        timerCount = TextView(this).apply {
            text = "0 leituras/notificações capturadas"
            textSize = 12f
            setTextColor(0xFF526574.toInt())
            setPadding(0, dp(4), 0, dp(8))
        }
        card.addView(timerCount)

        val durations = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        DiscoveryTestDuration.allowedSeconds.forEach { seconds ->
            val label = when (seconds) {
                60 -> "1 min"
                120 -> "2 min"
                300 -> "5 min"
                else -> "10 min"
            }
            val button = Button(this).apply {
                text = label
                textSize = 11f
                isAllCaps = false
                isSelected = seconds == selectedDurationSeconds
                setOnClickListener {
                    selectedDurationSeconds = seconds
                    updateDurationButtons(durations)
                    updateTimerUi(store.activeSession(System.currentTimeMillis()))
                }
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (isSelected) 0xFFD7E9E3.toInt() else 0xFFE8EDF1.toInt()
                )
            }
            durations.addView(button, LinearLayout.LayoutParams(0, dp(43), 1f).apply {
                marginEnd = dp(4)
            })
        }
        card.addView(durations)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        startButton = Button(this).apply {
            text = "Iniciar teste"
            isAllCaps = false
            setOnClickListener { confirmStartSession() }
        }
        stopButton = Button(this).apply {
            text = "Finalizar"
            isAllCaps = false
            visibility = View.GONE
            setOnClickListener {
                store.finishTestSession(System.currentTimeMillis(), "Finalizado pelo usuário")
                handler.removeCallbacks(countdownTick)
                updateTimerUi(null)
            }
        }
        actions.addView(startButton, LinearLayout.LayoutParams(0, dp(48), 1f))
        actions.addView(stopButton, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
            marginStart = dp(8)
        })
        card.addView(actions)
        body.addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) })
        updateTimerUi(store.activeSession(System.currentTimeMillis()))
    }

    private fun updateDurationButtons(container: LinearLayout) {
        for (index in 0 until container.childCount) {
            val button = container.getChildAt(index) as? Button ?: continue
            val seconds = when (button.text.toString()) {
                "1 min" -> 60
                "2 min" -> 120
                "5 min" -> 300
                else -> 600
            }
            button.isSelected = seconds == selectedDurationSeconds
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (button.isSelected) 0xFFD7E9E3.toInt() else 0xFFE8EDF1.toInt()
            )
        }
    }

    private fun confirmStartSession() {
        AlertDialog.Builder(this)
            .setTitle("Iniciar captura cronometrada")
            .setMessage(
                "Durante o tempo escolhido, o aplicativo salvará localmente as notificações e leituras BLE recebidas, incluindo HEX que pode conter informações de saúde. Os dados não serão enviados à rede. Inicie a sessão somente quando estiver pronto para observar o GT3 MAX."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Iniciar e salvar") { _, _ ->
                val started = store.startTestSession(selectedDurationSeconds, System.currentTimeMillis())
                if (started) {
                    scheduleCountdown()
                    updateTimerUi(store.activeSession(System.currentTimeMillis()))
                } else {
                    AlertDialog.Builder(this)
                        .setMessage("Não foi possível iniciar. Verifique se já existe um teste ativo ou se a biblioteca está inválida/incompatível.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .show()
    }

    private fun scheduleCountdown() {
        handler.removeCallbacks(countdownTick)
        if (store.activeSession(System.currentTimeMillis()) != null) {
            handler.post(countdownTick)
        }
    }

    private fun updateTimerUi(session: com.luizmarquesarruda.relogio20.data.discovery.DiscoveryTestSession?) {
        if (!::timerValue.isInitialized) return
        if (session == null) {
            val completedId = activeSessionId
            activeSessionId = null
            val finished = if (completedId == null) null else {
                (store.read() as? DiscoveryLibraryReadResult.Available)
                    ?.snapshot?.records?.lastOrNull {
                        it.sessionId == completedId && it.kind == DiscoveryLibraryKind.SESSION_FINISHED
                    }
            }
            if (finished != null) {
                timerValue.text = "00:00"
                timerStatus.text = finished.detail
                timerCount.text = "Sessão concluída. Abra a biblioteca para consultar as capturas."
            } else {
                timerValue.text = formatRemaining(selectedDurationSeconds)
                timerStatus.text = "Pronto • aguardando início do teste."
                timerCount.text = "A captura está parada. Nenhum dado novo será salvo."
            }
            timerProgress.progress = if (finished != null) 100 else 0
            startButton.isEnabled = true
            stopButton.visibility = View.GONE
            return
        }
        activeSessionId = session.id
        val now = System.currentTimeMillis()
        val remaining = session.remainingSeconds(now)
        val elapsed = (now - session.startedAtMillis).coerceAtLeast(0L)
        val progress = ((elapsed * 100L) / (session.durationSeconds * 1_000L)).toInt().coerceIn(0, 100)
        val captured = store.recordsForSession(session.id)
        timerValue.text = formatRemaining(remaining)
        timerStatus.text = if (captured == 0) {
            "Teste ativo • aguardando notificações ou leituras do relógio."
        } else {
            "Teste ativo • os dados recebidos estão sendo salvos."
        }
        timerProgress.progress = progress
        timerCount.text = captured.toString() + " leituras/notificações capturadas nesta sessão"
        startButton.isEnabled = false
        stopButton.visibility = View.VISIBLE
    }

    private fun renderLibraryShortcut() {
        val button = Button(this).apply {
            text = "Abrir biblioteca interna"
            isAllCaps = false
            setOnClickListener { startActivity(Intent(this@FeatureDiscoveryActivity, DiscoveryLibraryActivity::class.java)) }
        }
        body.addView(button, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        ).apply { bottomMargin = dp(10) })
    }

    private fun renderSnapshot(snapshot: DiscoverySnapshot) {
        addSectionTitle("Descobertas GATT")
        addText(body, "Última atualização: " + formatDate(snapshot.updatedAtMillis), 12f, 0xFF526574.toInt())
        addText(
            body,
            snapshot.serviceCount.toString() + " serviços • " +
                snapshot.characteristicCount + " características • " +
                snapshot.traffic.sumOf { it.notificationCount } + " notificações observadas",
            13f,
            0xFF526574.toInt()
        )
        val capabilityFindings = snapshot.findings.filter { it.id.startsWith("capability:") }
        if (capabilityFindings.isNotEmpty()) {
            addSectionTitle("Matriz de recursos Bluetooth SIG")
            val advertised = capabilityFindings.count {
                it.status != GattCapabilityMatrix.State.NOT_ADVERTISED.label
            }
            val observed = capabilityFindings.count {
                it.status == GattCapabilityMatrix.State.HISTORICAL_TRAFFIC.label ||
                    it.status == GattCapabilityMatrix.State.TRAFFIC_OBSERVED.label
            }
            addMessage(
                advertised.toString() + " de " + capabilityFindings.size +
                    " perfis/capacidades padronizados anunciados; " + observed +
                    " com tráfego registrado. Um perfil ausente no GATT pode existir por protocolo proprietário."
            )
            capabilityFindings.forEach(::renderFindingCard)
        }

        addSectionTitle("Outras evidências GATT")
        val otherFindings = snapshot.findings.filterNot { it.id.startsWith("capability:") }
        if (otherFindings.isEmpty()) {
            addMessage("Nenhuma evidência adicional foi registrada neste inventário.")
        } else {
            otherFindings.forEach(::renderFindingCard)
        }

        addSectionTitle("Canais com tráfego")
        if (snapshot.traffic.isEmpty()) {
            addMessage("Nenhuma notificação foi recebida. Isso não prova ausência de sensores.")
        } else {
            snapshot.traffic.forEach { item ->
                addText(
                    body,
                    item.characteristicUuid + " • " + item.notificationCount +
                        " notificações • último pacote " + item.lastPayloadBytes + " bytes",
                    11f,
                    0xFF526574.toInt()
                )
            }
        }
    }

    private fun renderFindingCard(finding: com.luizmarquesarruda.relogio20.device.discovery.FeatureFinding) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(10), dp(13), dp(10))
            background = roundedSurface()
        }
        addText(card, finding.category.uppercase(Locale.ROOT), 10f, 0xFF687786.toInt(), true)
        addText(card, finding.title, 15f, 0xFF14212B.toInt(), true)
        addText(card, finding.status, 10f, statusColor(finding.status), true)
        addText(card, finding.detail, 12f, 0xFF526574.toInt())
        addText(card, "Evidência: " + finding.evidence, 10f, 0xFF687786.toInt())
        if (finding.observationCount > 0) {
            addText(card, "Observações: " + finding.observationCount, 10f, 0xFF687786.toInt())
        }
        body.addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) })
    }

    private fun renderExperimentGuide() {
        addSectionTitle("Roteiro de experimentos")
        val toggle = Button(this).apply {
            text = if (showExperiments) "Ocultar roteiro" else "Mostrar roteiro de sensores e funções"
            isAllCaps = false
            setOnClickListener {
                showExperiments = !showExperiments
                render()
            }
        }
        body.addView(toggle, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        ))
        if (showExperiments) {
            WatchExperimentCatalog.all.forEachIndexed { index, experiment ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), dp(9), dp(12), dp(9))
                    background = roundedSurface()
                }
                addText(card, (index + 1).toString() + ". " + experiment.title, 13f, 0xFF14212B.toInt(), true)
                addText(card, experiment.instruction, 11f, 0xFF526574.toInt())
                addText(card, "Evidência: " + experiment.expectedEvidence, 10f, 0xFF687786.toInt())
                body.addView(card, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(7) })
            }
        }
    }

    private fun addSectionTitle(value: String) {
        body.addView(TextView(this).apply {
            text = value
            textSize = 16f
            setTextColor(0xFF14212B.toInt())
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(5))
        })
    }

    private fun addMessage(value: String) {
        addText(body, value, 12f, 0xFF526574.toInt())
    }

    private fun addText(parent: LinearLayout, value: String, size: Float, color: Int, bold: Boolean = false) {
        parent.addView(TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(3), 0, dp(4))
        })
    }

    private fun statusColor(status: String): Int = when {
        status.contains("BLOQUEADO") -> 0xFF9B2C2C.toInt()
        status.contains("CANDIDATO") || status.contains("NÃO VALID") -> 0xFF946200.toInt()
        status.contains("OBSERVADO") -> 0xFF176B55.toInt()
        else -> 0xFF526574.toInt()
    }

    private fun roundedSurface(): GradientDrawable = GradientDrawable().apply {
        setColor(0xFFFFFFFF.toInt())
        setStroke(dp(1), 0xFFE0E6EB.toInt())
        cornerRadius = dp(14).toFloat()
    }

    private fun formatDate(timestamp: Long): String =
        if (timestamp <= 0L) "Horário não disponível"
        else SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    private fun formatRemaining(seconds: Int): String =
        String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

}
