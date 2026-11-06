package com.luizmarquesarruda.relogio20.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectAvailability
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectStepsRepository
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectStepsResult
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectOriginPolicy
import com.luizmarquesarruda.relogio20.data.preferences.AppPreferences
import com.luizmarquesarruda.relogio20.ui.RelogioUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/** Daily activity view. Health Connect aggregates are never attributed to the watch alone. */
class ActivityOverviewActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository by lazy { HealthConnectStepsRepository(this) }
    private lateinit var stepsValue: android.widget.TextView
    private lateinit var sourceStatus: android.widget.TextView

    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(repository.readPermissions)) {
            loadSteps()
        } else {
            stepsValue.text = "N/D"
            sourceStatus.text = "A permissão READ_STEPS não foi concedida."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val body = RelogioUi.install(this, "ATIVIDADE", "MOVIMENTO • PASSOS • EXERCÍCIOS")
        RelogioUi.section(body, "ATIVIDADE DE HOJE")
        val stepsCard = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(RelogioUi.dp(this@ActivityOverviewActivity, 14), RelogioUi.dp(this@ActivityOverviewActivity, 14),
                RelogioUi.dp(this@ActivityOverviewActivity, 14), RelogioUi.dp(this@ActivityOverviewActivity, 14))
            background = RelogioUi.rounded(this@ActivityOverviewActivity)
        }
        stepsCard.addView(RelogioUi.textView(this, "PASSOS", 10f, RelogioUi.accent, true))
        stepsValue = RelogioUi.textView(this, "N/D", 34f, RelogioUi.text, true)
        stepsCard.addView(stepsValue)
        sourceStatus = RelogioUi.textView(this, "Toque em Atualizar para consultar o Health Connect.", 11f)
        stepsCard.addView(sourceStatus)
        body.addView(stepsCard, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = RelogioUi.dp(this@ActivityOverviewActivity, 9) })
        RelogioUi.button(body, "Atualizar passos") { loadSteps() }

        RelogioUi.section(body, "OUTROS INDICADORES")
        RelogioUi.metric(body, "Distância", "N/D", "Aguardando origem validada de distância.")
        RelogioUi.metric(body, "Calorias", "N/D", "Não estimar calorias sem dados e regra documentados.")
        RelogioUi.metric(body, "Exercícios", "N/D", "Sessões e tipos de exercício ainda não possuem integração validada.")
        RelogioUi.message(body, "Os passos vêm de um total agregado do Health Connect e podem incluir telefone ou outros aplicativos autorizados. Eles não são apresentados como passos exclusivos do GT3 Maxx.")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun loadSteps() {
        stepsValue.text = "…"
        sourceStatus.text = "Consultando o Health Connect."
        scope.launch {
            try {
                when (repository.availability()) {
                    HealthConnectAvailability.PROVIDER_UNAVAILABLE -> {
                        stepsValue.text = "N/D"
                        sourceStatus.text = "Health Connect não está disponível neste aparelho."
                        return@launch
                    }
                    HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED -> {
                        stepsValue.text = "N/D"
                        sourceStatus.text = "Atualize o Health Connect para consultar os passos."
                        return@launch
                    }
                    HealthConnectAvailability.AVAILABLE -> Unit
                }
                if (!repository.hasReadPermission()) {
                    stepsValue.text = "N/D"
                    sourceStatus.text = "É necessária autorização READ_STEPS."
                    permissionLauncher.launch(repository.readPermissions)
                    return@launch
                }
                val originPolicy = if (AppPreferences.includesExternalHealthSources(this@ActivityOverviewActivity)) {
                    HealthConnectOriginPolicy.INCLUDE_EXTERNAL
                } else {
                    HealthConnectOriginPolicy.APP_ONLY
                }
                when (val result = repository.readTodaySteps(originPolicy = originPolicy)) {
                    is HealthConnectStepsResult.Success -> {
                        val count = result.snapshot.totalSteps
                        stepsValue.text = count?.let {
                            NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(it)
                        } ?: "N/D"
                        sourceStatus.text = if (count == null) {
                            "Sem registros no intervalo consultado. Ausência não foi convertida em zero."
                        } else {
                            "Health Connect • recebido às " + result.snapshot.receivedAt
                        }
                    }
                    HealthConnectStepsResult.PermissionRequired -> {
                        stepsValue.text = "N/D"
                        sourceStatus.text = "Permissão ausente ou revogada. Autorize novamente para consultar."
                    }
                    HealthConnectStepsResult.ProviderUnavailable -> {
                        stepsValue.text = "N/D"
                        sourceStatus.text = "Health Connect indisponível."
                    }
                    HealthConnectStepsResult.ProviderUpdateRequired -> {
                        stepsValue.text = "N/D"
                        sourceStatus.text = "Health Connect precisa de atualização."
                    }
                    is HealthConnectStepsResult.Failure -> {
                        stepsValue.text = "N/D"
                        sourceStatus.text = "Falha ao consultar passos: " + result.reason
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                stepsValue.text = "N/D"
                sourceStatus.text = "Não foi possível consultar os passos."
            }
        }
    }
}
