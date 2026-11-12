package com.luizmarquesarruda.relogio20.data.weather

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

/**
 * Local cache only. City and forecast values stay in app-private preferences;
 * no account, device identifier, or health record is sent to the weather provider.
 */
internal class WeatherPreferences(
    context: Context,
) {
    private val preferences =
        context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveLocation(location: WeatherLocation) {
        val value =
            JSONObject()
                .put("name", location.name)
                .put("admin1", location.admin1 ?: JSONObject.NULL)
                .put("country", location.country)
                .put("latitude", location.latitude)
                .put("longitude", location.longitude)
        preferences.edit { putString(KEY_LOCATION, value.toString()) }
    }

    fun readLocation(): WeatherLocation? {
        val raw = preferences.getString(KEY_LOCATION, null) ?: return null
        return runCatching {
            val value = JSONObject(raw)
            val latitude = value.optDouble("latitude", Double.NaN)
            val longitude = value.optDouble("longitude", Double.NaN)
            if (!latitude.isFinite() || !longitude.isFinite() ||
                latitude !in -90.0..90.0 || longitude !in -180.0..180.0
            ) {
                return@runCatching null
            }

            WeatherLocation(
                name = value.optString("name", "").takeIf { it.isNotBlank() } ?: return@runCatching null,
                admin1 = if (value.isNull("admin1")) null else value.optString("admin1").takeIf { it.isNotBlank() },
                country = value.optString("country", ""),
                latitude = latitude,
                longitude = longitude,
            )
        }.getOrNull()
    }

    fun saveSnapshot(snapshot: WeatherSnapshot) {
        val value =
            JSONObject()
                .put("locationName", snapshot.locationName)
                .put("latitude", snapshot.latitude)
                .put("longitude", snapshot.longitude)
                .put("timeZone", snapshot.timeZone)
                .put("observedAt", snapshot.observedAt)
                .put("fetchedAtEpochMillis", snapshot.fetchedAtEpochMillis)
                .putNullable("temperatureC", snapshot.temperatureC)
                .putNullable("apparentTemperatureC", snapshot.apparentTemperatureC)
                .putNullable("humidityPercent", snapshot.humidityPercent)
                .putNullable("precipitationMm", snapshot.precipitationMm)
                .putNullable("precipitationProbabilityNextHourPercent", snapshot.precipitationProbabilityNextHourPercent)
                .putNullable("weatherCode", snapshot.weatherCode)
                .putNullable("windSpeedKmh", snapshot.windSpeedKmh)
                .putNullable("windDirectionDegrees", snapshot.windDirectionDegrees)
                .putNullable("windGustsKmh", snapshot.windGustsKmh)
                .putNullable("todayMinimumC", snapshot.todayMinimumC)
                .putNullable("todayMaximumC", snapshot.todayMaximumC)
                .putNullable("todayPrecipitationProbabilityPercent", snapshot.todayPrecipitationProbabilityPercent)
                .put("sunrise", snapshot.sunrise ?: JSONObject.NULL)
                .put("sunset", snapshot.sunset ?: JSONObject.NULL)
        preferences.edit { putString(KEY_SNAPSHOT, value.toString()) }
    }

    fun readSnapshot(): WeatherSnapshot? {
        val raw = preferences.getString(KEY_SNAPSHOT, null) ?: return null
        return runCatching {
            val value = JSONObject(raw)
            val latitude = value.optDouble("latitude", Double.NaN)
            val longitude = value.optDouble("longitude", Double.NaN)
            val fetchedAt = value.optLong("fetchedAtEpochMillis", 0L)
            if (!latitude.isFinite() || !longitude.isFinite() || fetchedAt <= 0L) {
                return@runCatching null
            }
            WeatherSnapshot(
                locationName = value.optString("locationName", ""),
                latitude = latitude,
                longitude = longitude,
                timeZone = value.optString("timeZone", "UTC"),
                observedAt = value.optString("observedAt", ""),
                fetchedAtEpochMillis = fetchedAt,
                temperatureC = value.nullableDouble("temperatureC"),
                apparentTemperatureC = value.nullableDouble("apparentTemperatureC"),
                humidityPercent = value.nullableInt("humidityPercent"),
                precipitationMm = value.nullableDouble("precipitationMm"),
                precipitationProbabilityNextHourPercent = value.nullableInt("precipitationProbabilityNextHourPercent"),
                weatherCode = value.nullableInt("weatherCode"),
                windSpeedKmh = value.nullableDouble("windSpeedKmh"),
                windDirectionDegrees = value.nullableInt("windDirectionDegrees"),
                windGustsKmh = value.nullableDouble("windGustsKmh"),
                todayMinimumC = value.nullableDouble("todayMinimumC"),
                todayMaximumC = value.nullableDouble("todayMaximumC"),
                todayPrecipitationProbabilityPercent = value.nullableInt("todayPrecipitationProbabilityPercent"),
                sunrise = if (value.isNull("sunrise")) null else value.optString("sunrise").takeIf { it.isNotBlank() },
                sunset = if (value.isNull("sunset")) null else value.optString("sunset").takeIf { it.isNotBlank() },
            )
        }.getOrNull()
    }

    private fun JSONObject.putNullable(
        key: String,
        value: Number?,
    ): JSONObject = put(key, value ?: JSONObject.NULL)

    private fun JSONObject.nullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return optDouble(key, Double.NaN).takeIf { it.isFinite() }
    }

    private fun JSONObject.nullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
    }

    companion object {
        private const val PREFERENCES_NAME = "weather_sync_v1"
        private const val KEY_LOCATION = "selected_location"
        private const val KEY_SNAPSHOT = "last_weather_snapshot"
    }
}
