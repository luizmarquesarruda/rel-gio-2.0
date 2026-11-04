package com.luizmarquesarruda.relogio20

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.luizmarquesarruda.relogio20.data.discovery.FeatureDiscoveryReadResult
import com.luizmarquesarruda.relogio20.data.discovery.FeatureDiscoveryStore
import com.luizmarquesarruda.relogio20.device.discovery.DiscoverySnapshot
import com.luizmarquesarruda.relogio20.device.discovery.WatchExperimentCatalog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Read-only view of the locally collected GATT discovery catalog. */
class FeatureDiscoveryActivity : Activity() {
    private lateinit var body: LinearLayout
    private val store by lazy { FeatureDiscoveryStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::body.isInitialized) render()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
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
            text = "Autodescoberta BLE"
            textSize = 21f
            setTextColor(0xFF14212B.toInt())
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(8), 0, 0, 0)
        })
        root.addView(header)

        val intro = TextView(this).apply {
            text = "O catálogo é atualizado automaticamente quando o Android conclui a descoberta GATT do relógio."
            textSize = 13f
            setTextColor(0xFF526574.toInt())
            setPadding(0, dp(10), 0, dp(8))
        }
        root.addView(intro)

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
        when (val result = store.read()) {
            FeatureDiscoveryReadResult.Empty -> {
                addText(body, "Nenhuma descoberta registrada.", 20f, 0xFF14212B.toInt(), true)
                addText(
                    body,
                    "Conecte o GT3 MAX pela tela principal. Quando a conexão GATT for concluída, " +
                        "o aplicativo inventariará automaticamente os serviços e as características.",
                    14f,
                    0xFF526574.toInt()
                )
            }
            is FeatureDiscoveryReadResult.Available -> renderSnapshot(result.snapshot)
            FeatureDiscoveryReadResult.Corrupt -> {
                addText(body, "Catálogo local inválido.", 18f, 0xFF9B2C2C.toInt(), true)
                addText(
                    body,
                    "O conteúdo existente foi preservado e não será sobrescrito automaticamente. " +
                        "Exporte o diagnóstico e investigue a integridade local antes de reiniciar a descoberta.",
                    13f,
                    0xFF526574.toInt()
                )
            }
            is FeatureDiscoveryReadResult.UnsupportedSchema -> {
                addText(body, "Catálogo de versão mais nova.", 18f, 0xFF9B2C2C.toInt(), true)
                addText(
                    body,
                    "O schema " + result.version + " é mais novo que o suportado nesta versão. " +
                        "Os dados foram preservados sem sobrescrita.",
                    13f,
                    0xFF526574.toInt()
                )
            }
        }
    }

    private fun renderSnapshot(snapshot: DiscoverySnapshot) {
        addText(body, "Último inventário", 12f, 0xFF526574.toInt(), true)
        addText(body, formatDate(snapshot.updatedAtMillis), 17f, 0xFF14212B.toInt(), true)
        addText(
            body,
            snapshot.serviceCount.toString() + " serviços • " +
                snapshot.characteristicCount + " características • " +
                snapshot.traffic.sumOf { it.notificationCount } + " notificações observadas",
            13f,
            0xFF526574.toInt()
        )
        addText(body, "Descobertas", 18f, 0xFF14212B.toInt(), true)

        if (snapshot.findings.isEmpty()) {
            addText(body, "Nenhuma funcionalidade candidata foi identificada neste inventário.", 13f, 0xFF526574.toInt())
        } else {
            snapshot.findings.forEach { finding ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    background = roundedSurface()
                }
                addText(card, finding.category.uppercase(Locale.ROOT), 10f, 0xFF687786.toInt(), true)
                addText(card, finding.title, 16f, 0xFF14212B.toInt(), true)
                addText(card, finding.status, 11f, statusColor(finding.status), true)
                addText(card, finding.detail, 13f, 0xFF526574.toInt())
                addText(card, "Evidência: " + finding.evidence, 11f, 0xFF687786.toInt())
                if (finding.observationCount > 0) {
                    addText(card, "Observações: " + finding.observationCount, 11f, 0xFF687786.toInt())
                }
                body.addView(card, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(9) })
            }
        }

        addText(body, "Tráfego observado", 17f, 0xFF14212B.toInt(), true)
        if (snapshot.traffic.isEmpty()) {
            addText(
                body,
                "Nenhuma notificação foi recebida desde a captura do inventário. Isso não significa que o relógio não tenha outras funções.",
                13f,
                0xFF526574.toInt()
            )
        } else {
            snapshot.traffic.forEach { item ->
                addText(
                    body,
                    item.characteristicUuid + " • " + item.notificationCount +
                        " notificações • último pacote " + item.lastPayloadBytes + " bytes",
                    12f,
                    0xFF526574.toInt()
                )
            }
        }

        renderExperimentGuide()

        addText(body, "Limites e segurança", 17f, 0xFF14212B.toInt(), true)
        addText(
            body,
            "A descoberta não lê características arbitrárias nem ativa novos canais de notificação. " +
                "A hora só é enviada pela característica Bluetooth CTS padrão 0x1805/0x2A2B, e apenas " +
                "quando ela anuncia WRITE. A inscrição NUS TX existente continua condicionada ao fingerprint " +
                "do GT3 MAX Call. Escritas proprietárias, NUS RX, DFU, OTA e reset permanecem bloqueados. " +
                "O catálogo guarda metadados e contagens; o log de diagnóstico existente pode conter HEX " +
                "e só é incluído no TXT por exportação manual.",
            12f,
            0xFF526574.toInt()
        )
    }


    private fun renderExperimentGuide() {
        addText(body, "Laboratório de experimentos", 17f, 0xFF14212B.toInt(), true)
        addText(
            body,
            "Use este roteiro para correlacionar uma ação manual no relógio com o tráfego BLE observado. " +
                "O aplicativo não inicia medições nem envia comandos proprietários.",
            12f,
            0xFF526574.toInt()
        )
        WatchExperimentCatalog.all.forEachIndexed { index, experiment ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(9), dp(12), dp(9))
                background = roundedSurface()
            }
            addText(card, (index + 1).toString() + ". " + experiment.title, 14f, 0xFF14212B.toInt(), true)
            addText(card, experiment.instruction, 12f, 0xFF526574.toInt())
            addText(card, "Evidência necessária: " + experiment.expectedEvidence, 11f, 0xFF687786.toInt())
            body.addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(7) })
        }
    }

    private fun addText(
        parent: LinearLayout,
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) {
        parent.addView(TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(5))
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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
