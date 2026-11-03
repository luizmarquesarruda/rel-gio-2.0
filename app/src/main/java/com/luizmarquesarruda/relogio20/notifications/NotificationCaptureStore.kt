package com.luizmarquesarruda.relogio20.notifications

import android.content.Context
import java.util.ArrayDeque

internal data class NotificationPreview(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val body: String,
    val receivedAtMillis: Long
)

internal object NotificationCaptureSettings {
    private const val PREFS_NAME = "notification_capture"
    private const val KEY_ENABLED = "capture_enabled"
    private const val KEY_ALLOWED_PACKAGES = "allowed_packages"

    fun isEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun allowedPackages(context: Context): Set<String> =
        preferences(context).getStringSet(KEY_ALLOWED_PACKAGES, emptySet())
            ?.toSet()
            ?: emptySet()

    fun setAllowedPackages(context: Context, packages: Set<String>) {
        preferences(context).edit()
            .putStringSet(KEY_ALLOWED_PACKAGES, packages.toSet())
            .apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

internal object NotificationCaptureStore {
    private const val MAX_EVENTS = 30
    private val lock = Any()
    private val events = ArrayDeque<NotificationPreview>(MAX_EVENTS)

    fun add(event: NotificationPreview) {
        synchronized(lock) {
            events.removeAll { it.key == event.key }
            events.addFirst(event)
            while (events.size > MAX_EVENTS) events.removeLast()
        }
    }

    fun snapshot(): List<NotificationPreview> =
        synchronized(lock) { events.toList() }

    fun clear() {
        synchronized(lock) { events.clear() }
    }
}

internal object NotificationTextNormalizer {
    private const val MAX_LENGTH = 500

    fun normalize(value: CharSequence?): String =
        value?.toString()
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.take(MAX_LENGTH)
            .orEmpty()
}
