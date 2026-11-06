package com.luizmarquesarruda.relogio20.ui.screens

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Switch
import com.luizmarquesarruda.relogio20.data.preferences.AppPreferences
import com.luizmarquesarruda.relogio20.ui.RelogioUi

/** Single-purpose settings screen. Only persistent user preferences are shown here. */
class SettingsActivity : Activity() {
    private lateinit var body: android.widget.LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        body = RelogioUi.install(this, "CONFIGURAÇÕES", "PREFERÊNCIAS DO APLICATIVO")
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::body.isInitialized) render()
    }

    private fun render() {
        body.removeAllViews()
        RelogioUi.section(body, "APRESENTAÇÃO")
        RelogioUi.card(
            body,
            "Formato do horário",
            "Define como o horário do telefone aparece no painel principal.",
            if (AppPreferences.uses24HourClock(this)) "24 HORAS" else "12 HORAS",
            "ALTERAR"
        ) {
            AlertDialog.Builder(this)
                .setTitle("Formato do horário")
                .setSingleChoiceItems(arrayOf("24 horas", "12 horas"), if (AppPreferences.uses24HourClock(this)) 0 else 1) { dialog, which ->
                    AppPreferences.set24HourClock(this, which == 0)
                    dialog.dismiss()
                    render()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        RelogioUi.card(
            body,
            "Unidade de distância",
            "Preferência de apresentação para uma futura métrica de distância validada.",
            if (AppPreferences.usesKilometers(this)) "QUILÔMETROS (KM)" else "MILHAS (MI)",
            "ALTERAR"
        ) {
            AlertDialog.Builder(this)
                .setTitle("Unidade de distância")
                .setSingleChoiceItems(arrayOf("Quilômetros (km)", "Milhas (mi)"), if (AppPreferences.usesKilometers(this)) 0 else 1) { dialog, which ->
                    AppPreferences.setKilometers(this, which == 0)
                    dialog.dismiss()
                    render()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        RelogioUi.section(body, "FONTES DE SAÚDE")
        val includeExternal = Switch(this).apply {
            text = "Incluir outras fontes do Health Connect"
            textSize = 13f
            setTextColor(RelogioUi.text)
            isChecked = AppPreferences.includesExternalHealthSources(this@SettingsActivity)
            setPadding(RelogioUi.dp(this@SettingsActivity, 4), RelogioUi.dp(this@SettingsActivity, 8),
                RelogioUi.dp(this@SettingsActivity, 4), RelogioUi.dp(this@SettingsActivity, 8))
            setOnCheckedChangeListener { _, checked ->
                AppPreferences.setIncludeExternalHealthSources(this@SettingsActivity, checked)
            }
        }
        body.addView(includeExternal)
        RelogioUi.message(
            body,
            "Desativado: consulta somente registros cuja origem seja o próprio Relógio 2.0. Ativado: permite que os totais agregados incluam telefone e outros aplicativos autorizados no Health Connect. O resultado continua identificado como agregado e nunca como dado exclusivo do GT3 Maxx."
        )

        RelogioUi.section(body, "ARMAZENAMENTO")
        RelogioUi.message(body, "As preferências desta tela ficam localmente neste aparelho. As alterações não enviam comandos BLE nem alteram o relógio.")
    }
}
