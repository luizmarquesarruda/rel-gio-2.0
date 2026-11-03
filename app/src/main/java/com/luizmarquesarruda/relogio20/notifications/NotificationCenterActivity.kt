package com.luizmarquesarruda.relogio20.notifications

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationCenterActivity : Activity() {
    private lateinit var statusView: TextView
    private lateinit var captureSwitch: Switch
    private lateinit var appList: LinearLayout
    private lateinit var eventsList: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val refreshTask = object : Runnable {
        override fun run() {
            renderEvents()
            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        renderEvents()
        handler.removeCallbacks(refreshTask)
        handler.post(refreshTask)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshTask)
        super.onPause()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(12))
            setBackgroundColor(0xFFF7F8FA.toInt())
        }

        root.addView(TextView(this).apply {
            text = "Notificações do Android"
            textSize = 23f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF17212B.toInt())
        })

        statusView = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF526170.toInt())
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(statusView)

        root.addView(makeButton("Conceder / revisar acesso a notificações") {
            runCatching {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        })

        captureSwitch = Switch(this).apply {
            text = "Capturar notificações dos aplicativos selecionados"
            textSize = 14f
            setTextColor(0xFF17212B.toInt())
            isChecked = NotificationCaptureSettings.isEnabled(this@NotificationCenterActivity)
            setOnCheckedChangeListener { _, enabled ->
                if (enabled && !hasNotificationAccess()) {
                    isChecked = false
                    statusView.text = "Conceda primeiro o acesso especial nas configurações do Android."
                } else {
                    NotificationCaptureSettings.setEnabled(this@NotificationCenterActivity, enabled)
                    statusView.text = if (enabled) {
                        "Captura ativa apenas para os aplicativos selecionados."
                    } else {
                        "Captura pausada."
                    }
                }
            }
        }
        root.addView(captureSwitch)

        root.addView(TextView(this).apply {
            text = "Aplicativos permitidos"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF17212B.toInt())
            setPadding(0, dp(14), 0, dp(4))
        })
        root.addView(TextView(this).apply {
            text = "Marque apenas os aplicativos cujas notificações deseja visualizar. WhatsApp e WhatsApp Business aparecem se estiverem instalados."
            textSize = 12f
            setTextColor(0xFF526170.toInt())
        })

        appList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(appList)

        root.addView(TextView(this).apply {
            text = "Pré-visualizações recentes (somente memória)"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF17212B.toInt())
            setPadding(0, dp(14), 0, dp(4))
        })

        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(makeButton("Limpar prévias") {
            NotificationCaptureStore.clear()
            renderEvents()
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        controls.addView(makeButton("Pausar captura") {
            NotificationCaptureSettings.setEnabled(this@NotificationCenterActivity, false)
            captureSwitch.isChecked = false
        }, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
            marginStart = dp(8)
        })
        root.addView(controls)

        eventsList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val eventScroll = ScrollView(this)
        eventScroll.addView(eventsList)
        root.addView(eventScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val note = TextView(this).apply {
            text = "O conteúdo fica apenas na memória do processo e é limitado às 30 notificações mais recentes. Nenhum texto é gravado no log ou enviado à rede. O encaminhamento ao GT3 MAX ainda não está disponível."
            textSize = 11f
            setTextColor(0xFF526170.toInt())
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(note)

        setContentView(root)
        renderAppChoices()
        refreshStatus()
    }

    private fun renderAppChoices() {
        appList.removeAllViews()
        val allowed = NotificationCaptureSettings.allowedPackages(this)
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(launcher, 0)
            .mapNotNull { resolve ->
                val info = resolve.activityInfo?.applicationInfo ?: return@mapNotNull null
                val packageName = info.packageName
                if (packageName == packageNameOfThisApp()) return@mapNotNull null
                val label = packageManager.getApplicationLabel(info).toString()
                packageName to label
            }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase(Locale.getDefault()) }

        if (apps.isEmpty()) {
            appList.addView(TextView(this).apply {
                text = "Nenhum aplicativo de usuário encontrado."
                textSize = 12f
                setTextColor(0xFF526170.toInt())
            })
            return
        }

        apps.forEach { (packageName, label) ->
            val checkBox = CheckBox(this).apply {
                text = "$label  •  $packageName"
                textSize = 12f
                setTextColor(0xFF263746.toInt())
                isChecked = packageName in allowed
                setOnCheckedChangeListener { _, checked ->
                    val next = NotificationCaptureSettings.allowedPackages(this@NotificationCenterActivity).toMutableSet()
                    if (checked) {
                        next.add(packageName)
                    } else {
                        next.remove(packageName)
                        NotificationCaptureStore.removePackage(packageName)
                    }
                    NotificationCaptureSettings.setAllowedPackages(this@NotificationCenterActivity, next)
                    renderEvents()
                }
            }
            appList.addView(checkBox)
        }
    }

    private fun renderEvents() {
        if (!::eventsList.isInitialized) return
        eventsList.removeAllViews()
        val events = NotificationCaptureStore.snapshot()
        if (events.isEmpty()) {
            eventsList.addView(TextView(this).apply {
                text = "Nenhuma notificação capturada nesta sessão."
                textSize = 12f
                setTextColor(0xFF526170.toInt())
                setPadding(0, dp(8), 0, dp(8))
            })
            return
        }

        events.forEach { event ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFFFFFFFF.toInt())
                setPadding(dp(10), dp(8), dp(10), dp(8))
            }
            card.addView(TextView(this).apply {
                text = event.appLabel + "  •  " +
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.receivedAtMillis))
                textSize = 11f
                setTextColor(0xFF526170.toInt())
            })
            if (event.title.isNotBlank()) {
                card.addView(TextView(this).apply {
                    text = event.title
                    textSize = 14f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(0xFF17212B.toInt())
                })
            }
            if (event.body.isNotBlank()) {
                card.addView(TextView(this).apply {
                    text = event.body
                    textSize = 13f
                    setTextColor(0xFF263746.toInt())
                })
            }
            eventsList.addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) })
        }
    }

    private fun refreshStatus() {
        val access = hasNotificationAccess()
        statusView.text = if (access) {
            "Acesso Android autorizado. A captura só funciona quando ativada e com aplicativos selecionados."
        } else {
            "Acesso Android ainda não concedido. A captura permanece inativa."
        }
    }

    private fun hasNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        return enabled.split(':').any { entry ->
            ComponentName.unflattenFromString(entry)?.packageName == packageName
        }
    }

    private fun packageNameOfThisApp(): String = applicationContext.packageName

    private fun makeButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            setOnClickListener { action() }
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
