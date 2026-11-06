package com.luizmarquesarruda.relogio20.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.health.connect.client.PermissionController
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectAvailability
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectOriginPolicy
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectStepsHistoryResult
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectStepsRepository
import com.luizmarquesarruda.relogio20.data.preferences.AppPreferences
import com.luizmarquesarruda.relogio20.ui.RelogioUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Historical Health Connect aggregates; BLE diagnostic captures remain a separate dataset. */
class HistoryActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository by lazy { HealthConnectStepsRepository(this) }
    private lateinit var historyContainer: android.widget.LinearLayout
    private lateinit var status: android.widget.TextView

    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(repository.readPermissions)) loadHistory()
        else status.text = "Permissão READ_STEPS não concedida."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val body = RelogioUi.install(this, "HISTÓRICO", "CONSULTAS • DATAS • ORIGEM")
        RelogioUi.section(body, "PASSOS • ÚLTIMOS 7 DIAS")
        status = RelogioUi.textView(this, "Atualize para consultar o Health Connect.", 11f)
        body.addView(status)
        RelogioUi.button(body, "Atualizar histórico de passos") { loadHistory() }
        historyContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }
        body.addView(historyContainer)
        RelogioUi.section(body, "REGISTROS TÉCNICOS")
        RelogioUi.card(
            body,
            "Biblioteca de descobertas BLE",
            "Inventários GATT e leituras de diagnóstico. Este conteúdo não é histórico de saúde ou atividade.",
            "LABORATÓRIO",
            "ABRIR BIBLIOTECA"
        ) {
            startActivity(android.content.Intent(this, DiscoveryLibraryActivity::class.java))
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun loadHistory() {
        status.text = "Consultando os últimos sete dias…"
        historyContainer.removeAllViews()
        scope.launch {
            try {
                when (repository.availability()) {
                    HealthConnectAvailability.PROVIDER_UNAVAILABLE -> {
                        status.text = "Health Connect não está disponível neste aparelho."
                        return@launch
                    }
                    HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED -> {
                        status.text = "Atualize o Health Connect."
                        return@launch
                    }
                    HealthConnectAvailability.AVAILABLE -> Unit
                }
                if (!repository.hasReadPermission()) {
                    status.text = "É necessária autorização READ_STEPS."
                    permissionLauncher.launch(repository.readPermissions)
                    return@launch
                }
                val policy = if (AppPreferences.includesExternalHealthSources(this@HistoryActivity)) {
                    HealthConnectOriginPolicy.INCLUDE_EXTERNAL
                } else {
                    HealthConnectOriginPolicy.APP_ONLY
                }
                when (val result = repository.readStepsHistory(days = 7, originPolicy = policy)) {
                    is HealthConnectStepsHistoryResult.Success -> {
                        status.text = "Health Connect • agregado por dia • origem conforme Configurações"
                        val formatter = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"))
                        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-BR"))
                        result.days.forEach { day ->
                            val value = day.totalSteps?.let { formatter.format(it) } ?: "N/D"
                            RelogioUi.card(
                                historyContainer,
                                day.date.format(dateFormatter),
                                if (day.totalSteps == null) "Sem registro no intervalo." else "Total agregado do dia.",
                                value
                            )
                        }
                    }
                    HealthConnectStepsHistoryResult.PermissionRequired ->
                        status.text = "Permissão ausente ou revogada."
                    is HealthConnectStepsHistoryResult.Failure ->
                        status.text = "Falha ao consultar histórico: " + result.reason
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                status.text = "Não foi possível consultar o histórico."
            }
        }
    }
}
