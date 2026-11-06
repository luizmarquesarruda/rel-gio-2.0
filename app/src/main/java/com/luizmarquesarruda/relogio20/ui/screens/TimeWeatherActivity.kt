package com.luizmarquesarruda.relogio20.ui.screens

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import com.luizmarquesarruda.relogio20.data.weather.OpenMeteoWeatherRepository
import com.luizmarquesarruda.relogio20.data.weather.WeatherCodeLabels
import com.luizmarquesarruda.relogio20.data.weather.WeatherLocation
import com.luizmarquesarruda.relogio20.data.weather.WeatherPreferences
import com.luizmarquesarruda.relogio20.data.weather.WeatherSnapshot
import com.luizmarquesarruda.relogio20.data.weather.WeatherSyncScheduler
import com.luizmarquesarruda.relogio20.ui.RelogioUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeWeatherActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository = OpenMeteoWeatherRepository()
    private lateinit var preferences: WeatherPreferences
    private lateinit var phoneTimeView: TextView
    private lateinit var automaticTimeView: TextView
    private lateinit var cityInput: EditText
    private lateinit var selectedCityView: TextView
    private lateinit var weatherStatusView: TextView
    private lateinit var currentWeatherView: TextView
    private lateinit var weatherDetailsView: TextView
    private lateinit var dailyForecastView: TextView
    private lateinit var searchButton: Button
    private lateinit var refreshButton: Button
    private var busy = false

    private val clockTick = object : Runnable {
        override fun run() {
            updatePhoneClock()
            handler.postDelayed(this, CLOCK_REFRESH_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = WeatherPreferences(this)
        buildScreen()
        renderCachedWeather()
        updatePhoneClock()
        handler.post(clockTick)

        if (preferences.readLocation() != null) {
            val cached = preferences.readSnapshot()
            val cacheAge = cached?.let { System.currentTimeMillis() - it.fetchedAtEpochMillis }
                ?: Long.MAX_VALUE
            if (cacheAge > CACHE_REFRESH_AFTER_MS) {
                refreshWeather(showToast = false)
            } else {
                WeatherSyncScheduler.ensurePeriodic(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePhoneClock()
        renderCachedWeather()
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockTick)
        activityScope.cancel()
        super.onDestroy()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF4F6F8.toInt())
            setPadding(dp(18), dp(14), dp(18), dp(18))
        }
        RelogioUi.applyEdgeToEdge(
            activity = this,
            root = root,
            paddingLeftDp = 18,
            paddingTopDp = 14,
            paddingRightDp = 18,
            paddingBottomDp = 18,
            lightSystemBarIcons = true
        )
        val header = LinearLayout(this).apply {
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
        header.addView(back, LinearLayout.LayoutParams(dp(52), dp(48)))
        val title = TextView(this).apply {
            text = "Hora e clima"
            textSize = 22f
            setTextColor(0xFF14212B.toInt())
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(8), 0, 0, 0)
        }
        header.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(header)

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(22))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        phoneTimeView = textView(30f, 0xFF14212B.toInt(), bold = true)
        automaticTimeView = textView(13f, 0xFF526574.toInt())
        val clockCard = card()
        addText(clockCard, "HORÁRIO DO CELULAR", 11f, 0xFF526574.toInt(), true)
        clockCard.addView(phoneTimeView)
        clockCard.addView(automaticTimeView)
        val timeSettingsButton = Button(this).apply {
            text = "Configurar data e hora do Android"
            isAllCaps = false
            setOnClickListener {
                runCatching {
                    startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
                }.onFailure {
                    Toast.makeText(this@TimeWeatherActivity, "Não foi possível abrir as configurações de data e hora.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        clockCard.addView(timeSettingsButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        ))
        addText(
            clockCard,
            "A referência é a hora do Android. Quando a opção automática está ativa, o próprio sistema pode sincronizá-la pela rede móvel ou outra fonte configurada.",
            12f,
            0xFF526574.toInt()
        )
        content.addView(clockCard, cardParams())

        addText(content, "Previsão do tempo", 19f, 0xFF14212B.toInt(), true)
        addText(
            content,
            "Escolha a cidade para consultar as condições atuais e a previsão pela internet do celular.",
            13f,
            0xFF526574.toInt()
        )

        cityInput = EditText(this).apply {
            hint = "Digite uma cidade (ex.: Sorocaba)"
            setSingleLine(true)
            textSize = 15f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(0xFFFFFFFF.toInt())
        }
        content.addView(cityInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        ).apply { topMargin = dp(8) })

        val searchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        searchButton = Button(this).apply {
            text = "Buscar cidade"
            isAllCaps = false
            setOnClickListener { searchCity() }
        }
        searchRow.addView(searchButton, LinearLayout.LayoutParams(0, dp(48), 1f))
        refreshButton = Button(this).apply {
            text = "Atualizar clima"
            isAllCaps = false
            setOnClickListener { refreshWeather(showToast = true) }
        }
        searchRow.addView(refreshButton, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
            marginStart = dp(8)
        })
        content.addView(searchRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })

        selectedCityView = textView(14f, 0xFF14212B.toInt(), bold = true)
        weatherStatusView = textView(12f, 0xFF526574.toInt())
        currentWeatherView = textView(23f, 0xFF14212B.toInt(), bold = true)
        weatherDetailsView = textView(14f, 0xFF526574.toInt())
        dailyForecastView = textView(13f, 0xFF526574.toInt())

        val weatherCard = card()
        weatherCard.addView(selectedCityView)
        weatherCard.addView(weatherStatusView)
        weatherCard.addView(currentWeatherView)
        weatherCard.addView(weatherDetailsView)
        weatherCard.addView(dailyForecastView)
        content.addView(weatherCard, cardParams().apply { topMargin = dp(12) })

        val attribution = textView(11f, 0xFF176B5B.toInt()).apply {
            text = "Dados meteorológicos: Open-Meteo.com (CC BY 4.0)"
            isClickable = true
            setOnClickListener {
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, "https://open-meteo.com/".toUri()))
                }
            }
        }
        content.addView(attribution)
        val licenseAttribution = textView(11f, 0xFF176B5B.toInt()).apply {
            text = "Ver licença CC BY 4.0 • descrições meteorológicas traduzidas para português"
            isClickable = true
            setOnClickListener {
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, "https://creativecommons.org/licenses/by/4.0/".toUri()))
                }
            }
        }
        content.addView(licenseAttribution)
        addText(
            content,
            "São usados dados de previsão meteorológica, não medição de estação local. A busca envia o nome da cidade digitado e as coordenadas da cidade selecionada; não envia GPS atual nem dados de saúde.",
            11f,
            0xFF526574.toInt()
        )
        addText(
            content,
            "A atualização automática em segundo plano é aproximada: o Android pode adiá-la por economia de bateria ou falta de rede. O último resultado válido fica salvo localmente.",
            12f,
            0xFF526574.toInt()
        )
        addText(
            content,
            "Integração com o relógio: a hora só pode ser enviada por serviço CTS Bluetooth SIG compatível. A captura atual do GT3 MAXX não anuncia esse serviço. O protocolo de clima do firmware ainda não foi identificado; nenhum comando proprietário será enviado.",
            12f,
            0xFF8A4B08.toInt(),
            true
        )

        val bluetoothButton = Button(this).apply {
            text = "Abrir painel Bluetooth"
            isAllCaps = false
            setOnClickListener { startActivity(Intent(this@TimeWeatherActivity, MainActivity::class.java)) }
        }
        content.addView(bluetoothButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(48)
        ).apply { topMargin = dp(8) })
        preferences.readLocation()?.let { cityInput.setText(it.name) }
    }

    private fun searchCity() {
        val query = cityInput.text?.toString()?.trim().orEmpty()
        if (query.length < 2) {
            cityInput.error = "Digite pelo menos 2 caracteres."
            return
        }
        setBusy(true)
        weatherStatusView.text = "Buscando cidades…"
        activityScope.launch {
            try {
                val matches = repository.searchLocations(query)
                if (matches.isEmpty()) {
                    weatherStatusView.text = "Nenhuma cidade encontrada. Confira a grafia."
                } else {
                    showLocationChoices(matches)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                weatherStatusView.text = "Não foi possível buscar a cidade. Verifique a conexão de internet."
            } finally {
                setBusy(false)
            }
        }
    }

    private fun showLocationChoices(locations: List<WeatherLocation>) {
        val labels = locations.map { it.displayName }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Selecione a localidade")
            .setItems(labels) { _, index -> selectLocation(locations[index]) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun selectLocation(location: WeatherLocation) {
        preferences.saveLocation(location)
        selectedCityView.text = location.displayName
        weatherStatusView.text = "Localidade salva. Consultando previsão…"
        refreshWeather(showToast = false)
    }

    private fun refreshWeather(showToast: Boolean) {
        val location = preferences.readLocation()
        if (location == null) {
            weatherStatusView.text = "Busque e selecione uma cidade primeiro."
            return
        }
        if (busy) return
        setBusy(true)
        weatherStatusView.text = "Consultando previsão pela rede…"
        activityScope.launch {
            try {
                val snapshot = repository.fetchForecast(location)
                preferences.saveSnapshot(snapshot)
                renderSnapshot(snapshot)
                if (showToast) {
                    Toast.makeText(this@TimeWeatherActivity, "Previsão atualizada.", Toast.LENGTH_SHORT).show()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                weatherStatusView.text = "Falha na atualização. O último resultado salvo será mantido."
                renderCachedWeather(keepStatus = true)
            } finally {
                setBusy(false)
                WeatherSyncScheduler.ensurePeriodic(this@TimeWeatherActivity)
            }
        }
    }

    private fun renderCachedWeather(keepStatus: Boolean = false) {
        val location = preferences.readLocation()
        selectedCityView.text = location?.displayName ?: "Nenhuma cidade selecionada"
        val snapshot = preferences.readSnapshot()
        if (snapshot != null) {
            renderSnapshot(snapshot, keepStatus)
        } else if (!keepStatus) {
            currentWeatherView.text = "—"
            weatherDetailsView.text = "Sem previsão armazenada."
            dailyForecastView.text = ""
            weatherStatusView.text = "Escolha uma cidade e atualize a previsão."
        }
    }

    private fun renderSnapshot(snapshot: WeatherSnapshot, keepStatus: Boolean = false) {
        selectedCityView.text = snapshot.locationName
        val temperature = snapshot.temperatureC?.let { "${it.oneDecimal()} °C" } ?: "—"
        val apparent = snapshot.apparentTemperatureC?.let { "${it.oneDecimal()} °C" } ?: "—"
        currentWeatherView.text = "$temperature • ${WeatherCodeLabels.describe(snapshot.weatherCode)}"
        weatherDetailsView.text = buildString {
            append("Sensação: ").append(apparent)
            append("   Umidade: ").append(snapshot.humidityPercent?.let { "$it%" } ?: "—")
            append("\nChuva agora: ").append(snapshot.precipitationMm?.let { "${it.oneDecimal()} mm" } ?: "—")
            append("   Chance próxima hora: ")
                .append(snapshot.precipitationProbabilityNextHourPercent?.let { "$it%" } ?: "—")
            append("\nVento: ").append(snapshot.windSpeedKmh?.let { "${it.oneDecimal()} km/h" } ?: "—")
            append("   Rajadas: ").append(snapshot.windGustsKmh?.let { "${it.oneDecimal()} km/h" } ?: "—")
            append("\nDireção do vento: ")
                .append(snapshot.windDirectionDegrees?.let { "$it°" } ?: "—")
        }
        dailyForecastView.text = buildString {
            append("Hoje: mínima ")
                .append(snapshot.todayMinimumC?.let { "${it.oneDecimal()} °C" } ?: "—")
            append(" • máxima ")
                .append(snapshot.todayMaximumC?.let { "${it.oneDecimal()} °C" } ?: "—")
            append("\nProbabilidade máxima de chuva hoje: ")
                .append(snapshot.todayPrecipitationProbabilityPercent?.let { "$it%" } ?: "—")
            append("\nNascer do sol: ").append(snapshot.sunrise?.substringAfter('T') ?: "—")
            append(" • Pôr do sol: ").append(snapshot.sunset?.substringAfter('T') ?: "—")
        }
        if (!keepStatus) {
            val fetched = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                .format(Date(snapshot.fetchedAtEpochMillis))
            weatherStatusView.text = "Atualizado no celular em $fetched • fuso da previsão: ${snapshot.timeZone}"
        }
    }

    private fun updatePhoneClock() {
        val now = Date()
        phoneTimeView.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
        val date = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("pt", "BR")).format(now)
        val autoTime = Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME, 0) == 1
        val autoZone = Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME_ZONE, 0) == 1
        automaticTimeView.text = "$date\nHora automática do Android: ${if (autoTime) "ativada" else "desativada"} • fuso automático: ${if (autoZone) "ativado" else "desativado"}"
    }

    private fun setBusy(value: Boolean) {
        busy = value
        searchButton.isEnabled = !value
        refreshButton.isEnabled = !value
        searchButton.text = if (value) "Aguarde…" else "Buscar cidade"
        refreshButton.text = if (value) "Atualizando…" else "Atualizar clima"
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(0xFFFFFFFF.toInt())
        elevation = dp(1).toFloat()
    }

    private fun cardParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(12) }

    private fun addText(
        parent: LinearLayout,
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) {
        parent.addView(textView(size, color, bold).apply {
            text = value
            setPadding(0, dp(4), 0, dp(7))
        })
    }

    private fun textView(size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(3), 0, dp(5))
        }

    private fun rounded(color: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(16).toFloat()
        setStroke(dp(1), 0xFFE0E6EB.toInt())
    }

    private fun Double.oneDecimal(): String =
        String.format(Locale("pt", "BR"), "%.1f", this)

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val CLOCK_REFRESH_INTERVAL_MS = 1_000L
        private const val CACHE_REFRESH_AFTER_MS = 30L * 60L * 1_000L
    }
}
