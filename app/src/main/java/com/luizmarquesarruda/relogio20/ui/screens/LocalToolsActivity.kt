package com.luizmarquesarruda.relogio20.ui.screens

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.AlarmClock
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectStepsRepository
import com.luizmarquesarruda.relogio20.data.healthconnect.HealthConnectStepsResult
import com.luizmarquesarruda.relogio20.domain.activity.LocalFeaturePolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * Android-local tools. These actions never send commands to the GT3 MAX.
 * Alarm-clock handoff is performed by the Android Clock app; health reminders
 * are local notifications and are not synchronized with the watch.
 */
class LocalToolsActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var body: LinearLayout
    private lateinit var stepsStatus: TextView
    private lateinit var goalInput: EditText
    private lateinit var stopwatchValue: TextView
    private lateinit var timerValue: TextView
    private lateinit var breathingValue: TextView
    private var stopwatchStartedAt = 0L
    private var stopwatchAccumulated = 0L
    private var stopwatchRunning = false
    private var timer: CountDownTimer? = null
    private var breathingRunning = false
    private var breathingPhaseIndex = 0
    private var currentSteps: Long? = null

    private val stopwatchTick = object : Runnable {
        override fun run() {
            if (!stopwatchRunning || !::stopwatchValue.isInitialized) return
            val elapsed = stopwatchAccumulated + (SystemClock.elapsedRealtime() - stopwatchStartedAt)
            stopwatchValue.text = formatElapsed(elapsed)
            handler.postDelayed(this, 100L)
        }
    }

    private val breathingTick = object : Runnable {
        override fun run() {
            if (!breathingRunning || !::breathingValue.isInitialized) return
            val phases = listOf("Inspire", "Segure", "Expire")
            val seconds = listOf(4L, 2L, 6L)
            breathingValue.text = phases[breathingPhaseIndex] + " • " + seconds[breathingPhaseIndex] + " s"
            handler.postDelayed({
                if (breathingRunning) {
                    breathingPhaseIndex = (breathingPhaseIndex + 1) % phases.size
                    breathingTick.run()
                }
            }, seconds[breathingPhaseIndex] * 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        loadStepGoal()
        refreshSteps()
    }

    override fun onResume() {
        super.onResume()
        if (::body.isInitialized) refreshSteps()
    }

    override fun onDestroy() {
        timer?.cancel()
        handler.removeCallbacks(stopwatchTick)
        handler.removeCallbacks(breathingTick)
        breathingRunning = false
        ioScope.cancel()
        super.onDestroy()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(18))
            setBackgroundColor(0xFFF4F6F8.toInt())
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(Button(this).apply {
            text = "‹"
            textSize = 25f
            isAllCaps = false
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(52), dp(48)))
        header.addView(TextView(this).apply {
            text = "Ferramentas locais"
            textSize = 22f
            setTextColor(0xFF14212B.toInt())
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(8), 0, 0, 0)
        })
        root.addView(header)

        val scroll = ScrollView(this).apply { isFillViewport = true }
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(24))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)

        addText("Estas funções executam no Android. Nenhuma delas envia comandos BLE ao relógio.", 13f)
        addStepGoalSection()
        addStopwatchSection()
        addTimerSection()
        addBreathingSection()
        addAlarmSection()
        addHealthReminderSection()
    }

    private fun addStepGoalSection() {
        section("Meta diária de passos")
        stepsStatus = addText("Consultando o agregado local do Health Connect…", 13f)
        goalInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            singleLine = true
            hint = "Meta de passos (padrão 8000)"
        }
        goalInput.setText(getSharedPreferences(PREFS, MODE_PRIVATE)
            .getInt(KEY_STEP_GOAL, LocalFeaturePolicy.DEFAULT_STEP_GOAL).toString())
        body.addView(goalInput, matchWrap())
        button("Salvar meta") {
            val goal = goalInput.text.toString().toIntOrNull()
            if (goal == null) {
                toast("Informe uma meta numérica.")
            } else {
                runCatching { LocalFeaturePolicy.normalizeStepGoal(goal) }
                    .onSuccess {
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putInt(KEY_STEP_GOAL, it).apply()
                        updateStepProgress()
                        toast("Meta diária salva.")
                    }
                    .onFailure { toast(it.message ?: "Meta inválida.") }
            }
        }
    }

    private fun refreshSteps() {
        if (!::stepsStatus.isInitialized) return
        ioScope.launch {
            val result = runCatching {
                val repository = HealthConnectStepsRepository(applicationContext)
                if (!repository.hasReadPermission()) {
                    HealthConnectStepsResult.PermissionRequired
                } else {
                    repository.readTodaySteps()
                }
            }.getOrElse { HealthConnectStepsResult.Failure(it.message ?: "Falha ao consultar passos.") }
            runOnUiThread {
                when (result) {
                    is HealthConnectStepsResult.Success -> {
                        currentSteps = result.snapshot.totalSteps
                        updateStepProgress()
                    }
                    HealthConnectStepsResult.PermissionRequired ->
                        stepsStatus.text = "Passos indisponíveis: conceda READ_STEPS no painel principal. Ausência não significa zero."
                    HealthConnectStepsResult.ProviderUnavailable ->
                        stepsStatus.text = "Health Connect não está disponível neste aparelho."
                    HealthConnectStepsResult.ProviderUpdateRequired ->
                        stepsStatus.text = "Atualize o Health Connect para consultar passos."
                    is HealthConnectStepsResult.Failure ->
                        stepsStatus.text = "Falha ao consultar passos: " + result.reason
                }
            }
        }
    }

    private fun updateStepProgress() {
        val goal = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getInt(KEY_STEP_GOAL, LocalFeaturePolicy.DEFAULT_STEP_GOAL)
        val current = currentSteps
        stepsStatus.text = if (current == null) {
            "Sem agregado disponível. O aplicativo não substitui ausência por zero."
        } else {
            val formatted = NumberFormat.getIntegerInstance(Locale.getDefault()).format(current)
            val remaining = LocalFeaturePolicy.remainingSteps(current, goal) ?: 0L
            "Hoje: $formatted passos • meta: $goal • faltam: $remaining • " +
                LocalFeaturePolicy.progressPercent(current, goal) + "%"
        }
    }

    private fun addStopwatchSection() {
        section("Cronômetro")
        stopwatchValue = addText("00:00.0", 28f)
        val row = horizontal()
        row.addView(actionButton("Iniciar") {
            if (!stopwatchRunning) {
                stopwatchStartedAt = SystemClock.elapsedRealtime()
                stopwatchRunning = true
                handler.removeCallbacks(stopwatchTick)
                handler.post(stopwatchTick)
            }
        }, weighted())
        row.addView(actionButton("Pausar") {
            if (stopwatchRunning) {
                stopwatchAccumulated += SystemClock.elapsedRealtime() - stopwatchStartedAt
                stopwatchRunning = false
                handler.removeCallbacks(stopwatchTick)
                stopwatchValue.text = formatElapsed(stopwatchAccumulated)
            }
        }, weighted())
        row.addView(actionButton("Zerar") {
            stopwatchRunning = false
            stopwatchAccumulated = 0L
            handler.removeCallbacks(stopwatchTick)
            stopwatchValue.text = "00:00.0"
        }, weighted())
        body.addView(row)
    }

    private fun addTimerSection() {
        section("Temporizador")
        timerValue = addText("Temporizador parado", 20f)
        val minutes = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            singleLine = true
            hint = "Minutos (1 a 180)"
        }
        body.addView(minutes, matchWrap())
        val row = horizontal()
        row.addView(actionButton("Iniciar") {
            val value = minutes.text.toString().toIntOrNull()
            if (value == null || value !in 1..180) {
                toast("Escolha entre 1 e 180 minutos.")
            } else {
                timer?.cancel()
                timer = object : CountDownTimer(value * 60_000L, 1_000L) {
                    override fun onTick(millisUntilFinished: Long) {
                        timerValue.text = formatRemaining(millisUntilFinished)
                    }
                    override fun onFinish() {
                        timerValue.text = "Tempo concluído"
                        toast("Temporizador concluído.")
                    }
                }.start()
            }
        }, weighted())
        row.addView(actionButton("Cancelar") {
            timer?.cancel()
            timerValue.text = "Temporizador parado"
        }, weighted())
        body.addView(row)
    }

    private fun addBreathingSection() {
        section("Treino respiratório guiado")
        breathingValue = addText("Pronto • ciclo 4–2–6", 20f)
        val row = horizontal()
        row.addView(actionButton("Iniciar") {
            if (!breathingRunning) {
                breathingRunning = true
                breathingPhaseIndex = 0
                breathingTick.run()
            }
        }, weighted())
        row.addView(actionButton("Parar") {
            breathingRunning = false
            handler.removeCallbacks(breathingTick)
            breathingValue.text = "Pausado"
        }, weighted())
        body.addView(row)
        addText("Guia de respiração sem medição fisiológica ou orientação clínica.", 11f)
    }

    private fun addAlarmSection() {
        section("Alarme do Android")
        addText("Abre o aplicativo Relógio do Android para você confirmar o alarme. Não sincroniza com o GT3 MAX.", 12f)
        button("Configurar alarme no Android") {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, 7)
                .putExtra(AlarmClock.EXTRA_MINUTES, 0)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            if (intent.resolveActivity(packageManager) != null) startActivity(intent)
            else toast("Nenhum aplicativo de relógio compatível foi encontrado.")
        }
    }

    private fun addHealthReminderSection() {
        section("Lembrete diário de bem-estar")
        addText("Notificação local no telefone. Não é enviada ao relógio e pode exigir reativação após reiniciar o aparelho.", 12f)
        button("Escolher horário do lembrete") {
            val now = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute -> requestAndScheduleReminder(hour, minute) },
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }
        button("Desativar lembrete") {
            cancelHealthReminder()
            toast("Lembrete desativado.")
        }
    }

    private fun requestAndScheduleReminder(hour: Int, minute: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingReminderHour = hour
            pendingReminderMinute = minute
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
            return
        }
        scheduleHealthReminder(hour, minute)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                scheduleHealthReminder(pendingReminderHour, pendingReminderMinute)
            } else {
                toast("Permissão de notificações negada; lembrete não agendado.")
            }
        }
    }

    private fun scheduleHealthReminder(hour: Int, minute: Int) {
        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .putBoolean(KEY_REMINDER_ENABLED, true)
            .apply()
        manager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            reminderPendingIntent()
        )
        toast(String.format(Locale.getDefault(), "Lembrete diário definido para %02d:%02d.", hour, minute))
    }

    private fun cancelHealthReminder() {
        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(reminderPendingIntent())
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(KEY_REMINDER_ENABLED, false).apply()
    }

    private fun reminderPendingIntent(): PendingIntent {
        val intent = Intent(this, LocalHealthReminderReceiver::class.java)
            .setAction(ACTION_HEALTH_REMINDER)
        return PendingIntent.getBroadcast(
            this, REQUEST_REMINDER, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun loadStepGoal() {
        if (::goalInput.isInitialized) {
            goalInput.setText(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(KEY_STEP_GOAL, LocalFeaturePolicy.DEFAULT_STEP_GOAL).toString())
        }
    }

    private fun section(label: String) {
        body.addView(TextView(this).apply {
            text = label
            textSize = 17f
            setTextColor(0xFF14212B.toInt())
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(18), 0, dp(7))
        })
    }

    private fun addText(value: String, size: Float): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(0xFF526574.toInt())
        setPadding(0, dp(4), 0, dp(6))
        body.addView(this, matchWrap())
    }

    private fun button(label: String, action: () -> Unit) {
        body.addView(actionButton(label, action), matchWrap())
    }

    private fun actionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }

    private fun horizontal() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(5) }
    private fun weighted() = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
        marginEnd = dp(4)
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun formatElapsed(ms: Long): String {
        val totalTenths = ms.coerceAtLeast(0L) / 100L
        val minutes = totalTenths / 600L
        val seconds = (totalTenths / 10L) % 60L
        val tenths = totalTenths % 10L
        return String.format(Locale.ROOT, "%02d:%02d.%d", minutes, seconds, tenths)
    }

    private fun formatRemaining(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L) + 999L) / 1_000L
        return String.format(Locale.ROOT, "%02d:%02d", totalSeconds / 60L, totalSeconds % 60L)
    }

    companion object {
        private const val PREFS = "local_feature_tools"
        private const val KEY_STEP_GOAL = "step_goal"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val REQUEST_NOTIFICATIONS = 4102
        private const val REQUEST_REMINDER = 4103
        private const val ACTION_HEALTH_REMINDER = "com.luizmarquesarruda.relogio20.LOCAL_HEALTH_REMINDER"
        private var pendingReminderHour = 9
        private var pendingReminderMinute = 0
    }
}

/** Delivers an Android-local notification; no watch transport is involved. */
class LocalHealthReminderReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val preferences = context.getSharedPreferences("local_feature_tools", Context.MODE_PRIVATE)
        if (!preferences.getBoolean("reminder_enabled", false)) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "local_health_reminders"
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Lembretes de bem-estar", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            android.app.Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pausa de bem-estar")
            .setContentText("Faça uma pausa, alongue-se e hidrate-se.")
            .setAutoCancel(true)
            .build()
        manager.notify(4104, notification)
    }
}
