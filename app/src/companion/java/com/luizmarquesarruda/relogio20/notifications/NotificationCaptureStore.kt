package com.luizmarquesarruda.relogio20.notifications

import android.content.Context
import android.os.SystemClock
import java.util.ArrayDeque

internal data class NotificationPreview(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val body: String,
    val receivedAtMillis: Long,
    val capturedAtElapsedRealtimeMillis: Long
)

internal object NotificationCaptureSettings {
    private const val PREFS_NAME = "notification_capture"
    private const val KEY_ENABLED = "capture_enabled"
    private const val KEY_ALLOWED_PACKAGES = "allowed_packages"

    fun isEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (!enabled) NotificationCaptureStore.clear()
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
    internal const val RETENTION_MILLIS = 15 * 60 * 1000L
    private val lock = Any()
    private val events = ArrayDeque<NotificationPreview>(MAX_EVENTS)

    fun add(
        event: NotificationPreview,
        nowElapsedRealtimeMillis: Long = SystemClock.elapsedRealtime()
    ) {
        synchronized(lock) {
            removeExpired(nowElapsedRealtimeMillis)
            events.removeAll { it.key == event.key }
            events.addFirst(event)
            while (events.size > MAX_EVENTS) events.removeLast()
        }
    }

    fun snapshot(
        nowElapsedRealtimeMillis: Long = SystemClock.elapsedRealtime()
    ): List<NotificationPreview> =
        synchronized(lock) {
            removeExpired(nowElapsedRealtimeMillis)
            events.toList()
        }

    fun remove(key: String) {
        synchronized(lock) { events.removeAll { it.key == key } }
    }

    fun removePackage(packageName: String) {
        synchronized(lock) { events.removeAll { it.packageName == packageName } }
    }

    fun millisUntilNextExpiry(
        nowElapsedRealtimeMillis: Long = SystemClock.elapsedRealtime()
    ): Long? = synchronized(lock) {
        removeExpired(nowElapsedRealtimeMillis)
        events.minOfOrNull {
            it.capturedAtElapsedRealtimeMillis + RETENTION_MILLIS
        }?.minus(nowElapsedRealtimeMillis)?.coerceAtLeast(0L)
    }

    fun clear() {
        synchronized(lock) { events.clear() }
    }

    private fun removeExpired(nowElapsedRealtimeMillis: Long) {
        events.removeAll {
            nowElapsedRealtimeMillis - it.capturedAtElapsedRealtimeMillis >= RETENTION_MILLIS
        }
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
