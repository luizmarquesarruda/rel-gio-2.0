package com.luizmarquesarruda.relogio20.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.luizmarquesarruda.relogio20.BuildConfig
import com.luizmarquesarruda.relogio20.ui.RelogioUi
import com.luizmarquesarruda.relogio20.notifications.NotificationCenterActivity

/** Secondary resource screen; settings deliberately live elsewhere. */
class MoreResourcesActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val body = RelogioUi.install(this, "MAIS RECURSOS", "LABORATÓRIO • FERRAMENTAS • INFORMAÇÕES")
        RelogioUi.section(body, "DIAGNÓSTICO DO RELÓGIO")
        RelogioUi.card(body, "Testes e descobertas BLE", "Captura autorizada, inventário GATT e experimentos de diagnóstico.", "TÉCNICO", "ABRIR") {
            startActivity(Intent(this, FeatureDiscoveryActivity::class.java))
        }
        RelogioUi.card(body, "Biblioteca interna", "Descobertas GATT e leituras recebidas em sessões de teste.", "LOCAL", "ABRIR") {
            startActivity(Intent(this, DiscoveryLibraryActivity::class.java))
        }
        RelogioUi.card(body, "Catálogo de funções", "Recursos agrupados por área e estado real de integração.", "INFORMATIVO", "ABRIR") {
            startActivity(Intent(this, FeatureHubActivity::class.java))
        }
        RelogioUi.section(body, "FERRAMENTAS DO TELEFONE")
        RelogioUi.card(body, "Ferramentas locais", "Utilitários executados no Android, sem enviar comandos proprietários ao relógio.", "ANDROID", "ABRIR") {
            startActivity(Intent(this, LocalToolsActivity::class.java))
        }
        RelogioUi.card(body, "Hora e clima", "Hora do telefone e consulta de previsão por cidade escolhida manualmente.", "REDE", "ABRIR") {
            startActivity(Intent(this, TimeWeatherActivity::class.java))
        }
        if (BuildConfig.FLAVOR == "companion") {
            RelogioUi.card(body, "Notificações", "Controles de acesso e prévias autorizadas pelo Android.", "COMPANION", "ABRIR") {
                startActivity(Intent(this, NotificationCenterActivity::class.java))
            }
        }
        RelogioUi.section(body, "PREFERÊNCIAS")
        RelogioUi.card(
            body,
            "Configurações",
            "Formato do horário, unidade de distância e escopo das fontes do Health Connect.",
            "PREFERÊNCIAS",
            "ABRIR"
        ) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        RelogioUi.section(body, "INFORMAÇÕES")
        RelogioUi.card(body, "Dados de saúde e privacidade", "Finalidade, permissões, fontes, armazenamento local e limites das medições.", "PRIVACIDADE", "ABRIR") {
            startActivity(Intent(this, HealthPrivacyActivity::class.java))
        }
    }
}
