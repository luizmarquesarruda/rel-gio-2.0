package com.luizmarquesarruda.relogio20.notifications

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** A variante diagnostic não declara nem implementa NotificationListenerService. */
class NotificationCenterActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "Captura de notificações indisponível nesta versão de diagnóstico. " +
                "Esta variante foi criada para validar Bluetooth LE e Health Connect sem acesso " +
                "especial às notificações do Android."
            textSize = 16f
            setPadding(32, 32, 32, 32)
        })
    }
}
