package com.luizmarquesarruda.relogio20.notifications

import android.app.Notification
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationCaptureService : NotificationListenerService() {

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        NotificationCaptureStore.remove(sbn.key)
    }

    override fun onListenerDisconnected() {
        NotificationCaptureStore.clear()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        NotificationCaptureStore.clear()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!NotificationCaptureSettings.isEnabled(applicationContext)) return
        if (sbn.packageName == packageName) return
        if (sbn.packageName !in NotificationCaptureSettings.allowedPackages(applicationContext)) {
            return
        }

        val extras = sbn.notification?.extras ?: return
        val title = NotificationTextNormalizer.normalize(
            extras.getCharSequence(Notification.EXTRA_TITLE)
        )
        val bigText = NotificationTextNormalizer.normalize(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        )
        val text = NotificationTextNormalizer.normalize(
            extras.getCharSequence(Notification.EXTRA_TEXT)
        )
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.mapNotNull { NotificationTextNormalizer.normalize(it).takeIf(String::isNotBlank) }
            .orEmpty()
        val body = when {
            bigText.isNotBlank() -> bigText
            text.isNotBlank() -> text
            lines.isNotEmpty() -> lines.joinToString(" • ").take(500)
            else -> ""
        }

        if (title.isBlank() && body.isBlank()) return

        val appLabel = runCatching {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(sbn.packageName)

        NotificationCaptureStore.add(
            NotificationPreview(
                key = sbn.key,
                packageName = sbn.packageName,
                appLabel = appLabel,
                title = title,
                body = body,
                receivedAtMillis = System.currentTimeMillis(),
                capturedAtElapsedRealtimeMillis = SystemClock.elapsedRealtime()
            )
        )
    }
}
