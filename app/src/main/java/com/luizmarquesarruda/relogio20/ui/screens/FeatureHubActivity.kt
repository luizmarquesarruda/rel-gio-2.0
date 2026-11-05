package com.luizmarquesarruda.relogio20.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.luizmarquesarruda.relogio20.notifications.NotificationCenterActivity

/**
 * Navegação secundária do catálogo GT3 MAX.
 *
 * Este catálogo não executa comandos proprietários. Os estados refletem o que está
 * implementado no app e o que ainda depende de validação física/protocolo.
 */
class FeatureHubActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var title: TextView

    private data class Section(
        val id: String,
        val name: String,
        val description: String
    )

    private data class Feature(
        val name: String,
        val description: String,
        val status: String,
        val available: Boolean = false,
        val blocked: Boolean = false,
        val route: String? = null
    )

    private val sections = listOf(
        Section("health", "Saúde", "Métricas, sono e bem-estar"),
        Section("activity", "Atividades", "Movimento, caminhadas e metas"),
        Section("device", "Dispositivo", "Tela, botões, sistema e manutenção"),
        Section("connectivity", "Conectividade", "Chamadas, áudio, mídia e notificações"),
        Section("tools", "Ferramentas", "Alarmes, cronômetro e utilitários"),
        Section("privacy", "Privacidade e dados", "Permissões, fontes e controles")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildShell()
        val sectionId = intent.getStringExtra(EXTRA_SECTION)
        if (sectionId == null) renderHome() else renderSection(sectionId)
    }

    private fun buildShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF4F6F8.toInt())
            setPadding(dp(18), dp(14), dp(18), dp(16))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = Button(this).apply {
            text = "‹"
            textSize = 26f
            isAllCaps = false
            minWidth = dp(48)
            setOnClickListener { finish() }
        }
        top.addView(back, LinearLayout.LayoutParams(dp(52), dp(48)))
        title = TextView(this).apply {
            text = "Funções do relógio"
            textSize = 22f
            setTextColor(0xFF14212B.toInt())
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(8), 0, 0, 0)
        }
        top.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(top)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(24))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private fun renderHome() {
        title.text = "Funções do relógio"
        body.removeAllViews()
        addText(body, "GT3 MAX", 12f, 0xFF526574.toInt(), true)
        addText(body, "Catálogo organizado por área", 24f, 0xFF14212B.toInt(), true)
        addText(
            body,
            "As funções aparecem com o estado real da integração. Itens ainda não validados não executam comandos no relógio.",
            14f,
            0xFF526574.toInt()
        )
        addCard(
            body,
            "Autodescoberta BLE",
            "Inventário automático de perfis GATT, canais encontrados e notificações realmente recebidas. Hipóteses permanecem separadas de funções validadas.",
            "ABRIR DESCOBERTA",
            false
        ) {
            startActivity(Intent(this, FeatureDiscoveryActivity::class.java))
        }
        sections.forEach { section ->
            addCard(
                body,
                section.name,
                section.description,
                "ABRIR",
                false
            ) {
                startActivity(Intent(this, FeatureHubActivity::class.java).putExtra(EXTRA_SECTION, section.id))
            }
        }
        addText(body, "Estado da integração", 17f, 0xFF14212B.toInt(), true)
        addCard(
            body,
            "Conexão BLE",
            "O painel inicial mantém a busca, conexão e inspeção GATT.",
            "PAINEL PRINCIPAL",
            false
        ) { finish() }
    }

    private fun renderSection(sectionId: String) {
        body.removeAllViews()
        val section = sections.firstOrNull { it.id == sectionId }
        if (section == null) {
            renderHome()
            return
        }
        title.text = section.name
        addText(body, section.description, 14f, 0xFF526574.toInt())
        addText(
            body,
            "Status informativo. A sincronização pelo Wearfit Pro não comprova que o Relógio 2.0 já decodifica e valida os mesmos dados diretamente.",
            12f,
            0xFF526574.toInt()
        )
        featuresFor(sectionId).forEach { feature ->
            addCard(
                body,
                feature.name,
                feature.description,
                feature.status,
                feature.blocked
            ) {
                when (feature.route) {
                    "notifications" -> startActivity(Intent(this, NotificationCenterActivity::class.java))
                    "privacy" -> startActivity(Intent(this, HealthPrivacyActivity::class.java))
                    else -> showFeatureInfo(feature)
                }
            }
        }
    }

    private fun featuresFor(id: String): List<Feature> = when (id) {
        "health" -> listOf(
            Feature("Frequência cardíaca", "Wearfit Pro recebe e sincroniza dados segundo o material do fabricante. Integração BLE própria e escrita Health Connect ainda pendentes.", "INTEGRAÇÃO DIRETA PENDENTE"),
            Feature("Oxigênio no sangue (SpO₂)", "Leitura óptica não diagnóstica. Parser próprio e validação independente pendentes.", "NÃO VALIDADO"),
            Feature("Pressão arterial", "O fabricante informa que o resultado é apenas referência. Não usar para diagnóstico ou tratamento.", "NÃO DIAGNÓSTICO • NÃO VALIDADO"),
            Feature("Sono", "Monitoramento descrito pelo fabricante; fases, horários e origem ainda precisam de validação no protocolo.", "INTEGRAÇÃO DIRETA PENDENTE"),
            Feature("Estresse / pressão", "Escala e significado exatos ainda precisam ser confirmados.", "SEMÂNTICA PENDENTE"),
            Feature("METT", "Equivalente metabólico (MET). Não converter automaticamente para calorias.", "UNIDADE DEFINIDA • SENSOR PENDENTE"),
            Feature("Treinamento respiratório", "Orientação e execução no relógio dependem de suporte confirmado.", "NÃO IMPLEMENTADO"),
            Feature("Ciclo fisiológico", "Dados sensíveis. Exige consentimento específico e controles de privacidade antes de qualquer integração.", "NÃO IMPLEMENTADO")
        )
        "activity" -> listOf(
            Feature("Reconhecimento automático", "O fabricante lista reconhecimento automático de movimento; início, fim e tipos ainda não são recebidos pelo app.", "NÃO VALIDADO"),
            Feature("Caminhadas e corridas", "Histórico de atividade e percurso no mapa ainda não implementados.", "NÃO IMPLEMENTADO"),
            Feature("Metas", "Metas locais de passos, distância e atividade ainda não implementadas.", "NÃO IMPLEMENTADO"),
            Feature("Histórico", "Não há histórico consolidado de sessões do GT3 MAX nesta versão.", "NÃO IMPLEMENTADO")
        )
        "device" -> listOf(
            Feature("Bateria do relógio", "Notificação NUS TX do GT3 MAX Call com interpretação AB 0x91/0x80. O valor só será confirmado após comparação com Wearfit Pro em diferentes níveis de carga.", "LEITURA CANDIDATA • NÃO VALIDADA"),
            Feature("Brilho da tela", "Ajuste remoto depende de comando proprietário identificado e validado.", "BLOQUEADO ATÉ VALIDAR"),
            Feature("Personalizar botão inferior", "Disponibilidade varia por modelo; mapeamento e comando não confirmados.", "SUPORTE INCERTO"),
            Feature("Bloqueio / desbloqueio", "Controle remoto de bloqueio não está habilitado.", "BLOQUEADO POR SEGURANÇA", blocked = true),
            Feature("Sincronização automática de hora", "O app identifica o perfil CTS 0x1805/0x2A2B no inventário, mas não escreve a hora do relógio nesta versão. A sincronização física continua pendente de implementação e validação seguras; use o Wearfit Pro até lá.", "ESCRITA CTS SUSPENSA • NÃO VALIDADO"),
            Feature("Reiniciar / desligar", "Comandos de energia não serão enviados sem protocolo e confirmação explícita.", "BLOQUEADO POR SEGURANÇA", blocked = true),
            Feature("Restaurar fábrica", "Risco de perda de dados e pareamento. Sem ação disponível.", "BLOQUEADO", blocked = true),
            Feature("Atualização OTA", "Firmware e assinatura não foram identificados. Não executar atualização por engenharia reversa.", "BLOQUEADO", blocked = true)
        )
        "connectivity" -> listOf(
            Feature("Notificações", "Tela de consentimento e seleção de aplicativos já existente na variante companion.", "TELA EXISTENTE", available = true, route = "notifications"),
            Feature("Chamadas", "Perfil de áudio Bluetooth Classic e sinalização de chamadas ainda não integrados.", "NÃO IMPLEMENTADO"),
            Feature("Áudio Bluetooth", "Perfis de áudio 1 e 2 listados pelo fabricante; roteamento ainda não implementado.", "NÃO IMPLEMENTADO"),
            Feature("Controle de música", "Controles de mídia do telefone ainda não integrados.", "NÃO IMPLEMENTADO"),
            Feature("Câmera remota", "Exige fluxo explícito e integração Android segura; não implementado.", "NÃO IMPLEMENTADO")
        )
        "tools" -> listOf(
            Feature("Alarmes", "Alarmes locais e sincronização com o relógio ainda não integrados.", "NÃO IMPLEMENTADO"),
            Feature("Cronômetro", "Função descrita pelo fabricante; tela própria ainda não criada.", "NÃO IMPLEMENTADO"),
            Feature("Temporizador", "Função descrita pelo fabricante; tela própria ainda não criada.", "NÃO IMPLEMENTADO"),
            Feature("Encontrar telefone", "Exige canal de comando validado e ação explícita.", "NÃO IMPLEMENTADO"),
            Feature("Cartão de visita", "Compartilhamento de dados pessoais não implementado.", "NÃO IMPLEMENTADO"),
            Feature("Quick collection", "Finalidade exata não confirmada; não atribuir semântica por tradução literal.", "SIGNIFICADO PENDENTE"),
            Feature("Alipay", "Pagamentos e carteiras digitais não serão implementados sem integração oficial e requisitos de segurança.", "FORA DO ESCOPO ATUAL", blocked = true),
            Feature("Cartão de acesso", "Emulação de credenciais de acesso não implementada.", "BLOQUEADO POR SEGURANÇA", blocked = true)
        )
        "privacy" -> listOf(
            Feature("Dados de saúde e privacidade", "Finalidades, permissões e limites da integração atual.", "TELA EXISTENTE", available = true, route = "privacy"),
            Feature("Permissões Android", "Permissões devem ser concedidas pelo usuário nas configurações do Android.", "CONTROLE DO ANDROID"),
            Feature("Fontes e qualidade dos dados", "A origem deve ser preservada: relógio, telefone ou agregado Health Connect.", "CONTRATO DE DOMÍNIO CRIADO")
        )
        else -> emptyList()
    }

    private fun showFeatureInfo(feature: Feature) {
        val state = when {
            feature.blocked -> "Este recurso permanece bloqueado nesta versão."
            feature.available -> "Esta área já possui uma tela no aplicativo."
            else -> "Esta função está catalogada, mas ainda não está implementada ou validada."
        }
        Toast.makeText(this, state, Toast.LENGTH_LONG).show()
    }

    private fun addCard(
        parent: LinearLayout,
        heading: String,
        description: String,
        status: String,
        warning: Boolean,
        action: () -> Unit
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(if (warning) 0xFFFFF4E5.toInt() else 0xFFFFFFFF.toInt())
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            elevation = dp(1).toFloat()
        }
        val headingView = TextView(this).apply {
            text = heading
            textSize = 17f
            setTextColor(0xFF14212B.toInt())
            setTypeface(typeface, Typeface.BOLD)
        }
        card.addView(headingView)
        val descriptionView = TextView(this).apply {
            text = description
            textSize = 13f
            setTextColor(0xFF526574.toInt())
            setPadding(0, dp(5), 0, dp(8))
        }
        card.addView(descriptionView)
        val statusView = TextView(this).apply {
            text = status
            textSize = 11f
            setTextColor(if (warning) 0xFF8A4B08.toInt() else 0xFF176B55.toInt())
            setTypeface(typeface, Typeface.BOLD)
        }
        card.addView(statusView)
        parent.addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) })
    }

    private fun addText(
        parent: LinearLayout,
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) {
        val view = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(5), 0, dp(7))
        }
        parent.addView(view)
    }

    private fun rounded(color: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(16).toFloat()
        setStroke(dp(1), 0xFFE0E6EB.toInt())
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val EXTRA_SECTION = "com.luizmarquesarruda.relogio20.EXTRA_SECTION"
    }
}
