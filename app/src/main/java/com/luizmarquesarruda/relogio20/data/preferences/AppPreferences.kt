package com.luizmarquesarruda.relogio20.data.preferences

import android.content.Context
import androidx.core.content.edit

/** Preferences for presentation and the explicit scope of Health Connect aggregates. */
object AppPreferences {
    private const val FILE_NAME = "relogio2_user_preferences"
    private const val KEY_CLOCK_24H = "clock_24h"
    private const val KEY_DISTANCE_KM = "distance_km"
    private const val KEY_INCLUDE_EXTERNAL_HEALTH = "include_external_health_sources"
    private const val LEGACY_FILE_NAME = "local_feature_tools"
    private const val LEGACY_KEY_INCLUDE_EXTERNAL = "include_external_health_origins"

    fun uses24HourClock(context: Context): Boolean = preferences(context).getBoolean(KEY_CLOCK_24H, true)

    fun set24HourClock(
        context: Context,
        enabled: Boolean,
    ) {
        preferences(context).edit { putBoolean(KEY_CLOCK_24H, enabled) }
    }

    fun usesKilometers(context: Context): Boolean = preferences(context).getBoolean(KEY_DISTANCE_KM, true)

    fun setKilometers(
        context: Context,
        enabled: Boolean,
    ) {
        preferences(context).edit { putBoolean(KEY_DISTANCE_KM, enabled) }
    }

    fun includesExternalHealthSources(context: Context): Boolean {
        val current = preferences(context)
        if (current.contains(KEY_INCLUDE_EXTERNAL_HEALTH)) {
            return current.getBoolean(KEY_INCLUDE_EXTERNAL_HEALTH, false)
        }
        val legacyValue =
            context.applicationContext
                .getSharedPreferences(LEGACY_FILE_NAME, Context.MODE_PRIVATE)
                .getBoolean(LEGACY_KEY_INCLUDE_EXTERNAL, false)
        current.edit { putBoolean(KEY_INCLUDE_EXTERNAL_HEALTH, legacyValue) }
        return legacyValue
    }

    fun setIncludeExternalHealthSources(
        context: Context,
        enabled: Boolean,
    ) {
        preferences(context).edit { putBoolean(KEY_INCLUDE_EXTERNAL_HEALTH, enabled) }
    }

    private fun preferences(context: Context) = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
}
