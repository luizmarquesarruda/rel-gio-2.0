package com.luizmarquesarruda.relogio20.notifications

import android.content.Context
import androidx.core.content.edit
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
        NotificationCaptureStore.updateSettings {
            preferences(context).edit { putBoolean(KEY_ENABLED, enabled) }
            if (!enabled) NotificationCaptureStore.clearLocked()
        }
    }

    fun allowedPackages(context: Context): Set<String> =
        preferences(context).getStringSet(KEY_ALLOWED_PACKAGES, emptySet())
            ?.toSet()
            ?: emptySet()

    fun setAllowedPackages(context: Context, packages: Set<String>) {
        val allowed = packages.toSet()
        NotificationCaptureStore.updateSettings {
            preferences(context).edit { putStringSet(KEY_ALLOWED_PACKAGES, allowed) }
            NotificationCaptureStore.removeDisallowedPackagesLocked(allowed)
        }
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
        synchronized(lock) { addLocked(event, nowElapsedRealtimeMillis) }
    }

    /**
     * Validates consent and package allow-list while holding the same lock used by
     * pause/revoke/settings updates. A callback already in progress cannot repopulate
     * previews after the user disables capture.
     */
    fun addIfAllowed(
        event: NotificationPreview,
        isAllowed: () -> Boolean,
        nowElapsedRealtimeMillis: Long = SystemClock.elapsedRealtime()
    ): Boolean = synchronized(lock) {
        if (!runCatching(isAllowed).getOrDefault(false)) return@synchronized false
        addLocked(event, nowElapsedRealtimeMillis)
        true
    }

    internal fun updateSettings(update: () -> Unit) {
        synchronized(lock) { update() }
    }

    internal fun clearLocked() {
        events.clear()
    }

    internal fun removeDisallowedPackagesLocked(allowedPackages: Set<String>) {
        events.removeAll { it.packageName !in allowedPackages }
    }

    private fun addLocked(event: NotificationPreview, nowElapsedRealtimeMillis: Long) {
        removeExpired(nowElapsedRealtimeMillis)
        events.removeAll { it.key == event.key }
        events.addFirst(event)
        while (events.size > MAX_EVENTS) events.removeLast()
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
        synchronized(lock) { clearLocked() }
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
