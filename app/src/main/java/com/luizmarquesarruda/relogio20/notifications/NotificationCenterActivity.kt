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

        root.addView(TextView(this).apply {
            text = "Privacidade: o acesso especial do Android permite que este app receba notificações do sistema. O Relógio 2.0 só processa notificações dos aplicativos que você marcar e somente depois de ativar a captura. As prévias ficam temporariamente na memória, limitadas a 30 itens por até 15 minutos; são apagadas quando o listener desconecta, quando a notificação é removida ou quando você limpa a lista. O conteúdo não é gravado em arquivos nem enviado à rede. Não conceda o acesso se não concordar com esse tratamento."
            textSize = 12f
            setTextColor(0xFF526170.toInt())
            setPadding(0, dp(4), 0, dp(10))
        })

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
                    NotificationCaptureSettings.setEnabled(this@NotificationCenterActivity, false)
                    statusView.text = "Conceda primeiro o acesso especial nas configurações do Android."
                } else if (enabled && NotificationCaptureSettings.allowedPackages(this@NotificationCenterActivity).isEmpty()) {
                    isChecked = false
                    NotificationCaptureSettings.setEnabled(this@NotificationCenterActivity, false)
                    statusView.text = "Selecione ao menos um aplicativo antes de ativar a captura."
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
        val appScroll = ScrollView(this).apply { addView(appList) }
        root.addView(appScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(160)
        ))

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
            text = "As prévias ficam apenas na memória: no máximo 30 itens por até 15 minutos. O texto não é gravado em logs ou arquivos e não é enviado à rede. O encaminhamento ao GT3 MAX ainda não está disponível."
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
                    if (next.isEmpty()) {
                        NotificationCaptureSettings.setEnabled(this@NotificationCenterActivity, false)
                        captureSwitch.isChecked = false
                        statusView.text = "Nenhum aplicativo selecionado. Captura pausada."
                    }
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
        if (!access) {
            NotificationCaptureSettings.setEnabled(this, false)
            NotificationCaptureStore.clear()
            if (::captureSwitch.isInitialized) captureSwitch.isChecked = false
            statusView.text = "Acesso Android ainda não concedido. A captura permanece desativada."
            return
        }
        statusView.text =
            "Acesso Android autorizado. A captura só funciona após ativação explícita e seleção de aplicativos."
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
