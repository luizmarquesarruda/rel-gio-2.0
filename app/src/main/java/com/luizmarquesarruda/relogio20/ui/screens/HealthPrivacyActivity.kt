package com.luizmarquesarruda.relogio20.ui.screens

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Disclosure visível ao usuário sobre dados de saúde e notificações.
 * O estado descrito corresponde às funções existentes nesta versão.
 */
class HealthPrivacyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content = """
            DADOS DE SAÚDE E PRIVACIDADE

            FINALIDADE
            O Relógio 2.0 é um aplicativo de Saúde e Fitness para acompanhar dados de um smartwatch e compartilhar informações autorizadas com a plataforma Conexão Saúde (Health Connect).

            DADOS DE ATIVIDADE
            Nesta versão, o app solicita somente READ_STEPS para consultar o total agregado de passos no Health Connect. Esse total pode incluir o telefone e outras fontes conectadas; não deve ser interpretado como dado exclusivo do relógio.

            DADOS DIRETOS DO RELÓGIO
            O objetivo é receber amostras dos sensores do relógio, validar origem, unidade e horário, e então gravar somente dados reais com as permissões WRITE específicas. A integração direta de passos, frequência cardíaca, sono e SpO₂ do GT3 Maxx ainda não está validada. Nenhum valor fictício é gravado.

            ARMAZENAMENTO E COMPARTILHAMENTO
            Health Connect mantém dados de forma centrada no dispositivo e controla quais aplicativos podem ler ou gravar cada tipo. Esta versão não envia dados de saúde para servidores do desenvolvedor nem faz upload automático para a nuvem da Conta Google. Outros aplicativos compatíveis podem acessar os dados no Health Connect conforme as permissões que o usuário conceder.

            HISTÓRICO DE DIAGNÓSTICO BLE
            Leituras e quadros HEX do relógio somente são capturados dentro de uma sessão local cronometrada, após ativação do usuário. Depois de salvos, permanecem no arquivo privado do aplicativo, limitados a 1.000 registros; não expiram automaticamente ao fim da sessão. Esta versão ainda não oferece exclusão individual dos registros. Remover os dados do aplicativo nas configurações do Android elimina esse histórico. O conteúdo não é enviado à rede nem ao Health Connect.

            NOTIFICAÇÕES DO CELULAR
            Na variante companion, o usuário pode conceder acesso especial do Android, escolher aplicativos e ativar a leitura. O Relógio 2.0 não responde, apaga, arquiva ou altera notificações nos aplicativos de origem. As prévias ficam somente em memória, limitadas a 30 itens por até 15 minutos; pausar a captura ou revogar o acesso apaga imediatamente as prévias em memória. O encaminhamento ao GT3 Maxx ainda aguarda validação do protocolo.

            CONTROLES
            O usuário pode negar ou revogar permissões no Android/Health Connect, desativar a leitura de notificações, remover aplicativos permitidos e limpar prévias. As funções que dependem de permissões negadas permanecem indisponíveis.

            LIMITES
            O Relógio 2.0 é uma ferramenta de atividade e bem-estar, não um dispositivo médico. Leituras de relógios não substituem avaliação clínica nem equipamentos médicos validados. Não use estimativas de pressão, SpO₂ ou frequência cardíaca para diagnóstico.
        """.trimIndent()

        val title = TextView(this).apply {
            text = "Dados de saúde e privacidade"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF17212B.toInt())
            setPadding(dp(18), dp(18), dp(18), dp(6))
        }
        val body = TextView(this).apply {
            text = content
            textSize = 15f
            setTextColor(0xFF263746.toInt())
            setPadding(dp(18), dp(18), dp(18), dp(24))
            setLineSpacing(dp(4).toFloat(), 1.0f)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF7F8FA.toInt())
            addView(title)
            addView(body)
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
