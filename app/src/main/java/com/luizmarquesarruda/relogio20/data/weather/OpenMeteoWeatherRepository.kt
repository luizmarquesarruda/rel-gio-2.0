package com.luizmarquesarruda.relogio20.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Open-Meteo adapter. Only the user-selected city coordinates are sent to the provider.
 * No Android device ID, watch ID, or health data is sent.
 */
internal class OpenMeteoWeatherRepository {
    suspend fun searchLocations(query: String): List<WeatherLocation> = withContext(Dispatchers.IO) {
        val normalized = query.trim()
        require(normalized.length in 2..80) { "Informe uma cidade com pelo menos 2 caracteres." }
        val encoded = URLEncoder.encode(normalized, "UTF-8")
        val root = JSONObject(
            getText(
                "https://geocoding-api.open-meteo.com/v1/search" +
                    "?name=$encoded&count=6&language=pt&format=json"
            )
        )
        val results = root.optJSONArray("results") ?: JSONArray()
        (0 until results.length()).mapNotNull { index ->
            val item = results.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("name", "").trim()
            val latitude = item.optDouble("latitude", Double.NaN)
            val longitude = item.optDouble("longitude", Double.NaN)
            if (name.isBlank() || !latitude.isFinite() || !longitude.isFinite() ||
                latitude !in -90.0..90.0 || longitude !in -180.0..180.0
            ) return@mapNotNull null
            WeatherLocation(
                name = name,
                admin1 = item.optString("admin1", "").takeIf { it.isNotBlank() },
                country = item.optString("country", ""),
                latitude = latitude,
                longitude = longitude
            )
        }
    }

    suspend fun fetchForecast(location: WeatherLocation): WeatherSnapshot = withContext(Dispatchers.IO) {
        require(location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0) {
            "Coordenadas fora dos limites."
        }
        val latitude = String.format(Locale.US, "%.5f", location.latitude)
        val longitude = String.format(Locale.US, "%.5f", location.longitude)
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature," +
                "precipitation,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m" +
            "&hourly=precipitation_probability" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset" +
            "&forecast_days=3&timezone=auto&wind_speed_unit=kmh"

        val root = JSONObject(getText(url))
        val current = root.optJSONObject("current")
            ?: throw IOException("A resposta meteorológica não contém condições atuais.")
        val hourly = root.optJSONObject("hourly") ?: JSONObject()
        val daily = root.optJSONObject("daily") ?: JSONObject()
        val observedAt = current.optString("time", "")
        val hourlyTimes = hourly.optJSONArray("time")
        val hourlyRain = hourly.optJSONArray("precipitation_probability")
        val nextHourIndex = findNextHour(hourlyTimes, observedAt)

        WeatherSnapshot(
            locationName = location.displayName,
            latitude = location.latitude,
            longitude = location.longitude,
            timeZone = root.optString("timezone", "UTC"),
            observedAt = observedAt,
            fetchedAtEpochMillis = System.currentTimeMillis(),
            temperatureC = current.nullableDouble("temperature_2m"),
            apparentTemperatureC = current.nullableDouble("apparent_temperature"),
            humidityPercent = current.nullableInt("relative_humidity_2m"),
            precipitationMm = current.nullableDouble("precipitation"),
            precipitationProbabilityNextHourPercent = nextHourIndex
                ?.let { hourlyRain?.nullableIntAt(it) },
            weatherCode = current.nullableInt("weather_code"),
            windSpeedKmh = current.nullableDouble("wind_speed_10m"),
            windDirectionDegrees = current.nullableInt("wind_direction_10m"),
            windGustsKmh = current.nullableDouble("wind_gusts_10m"),
            todayMinimumC = daily.arrayDoubleAt("temperature_2m_min", 0),
            todayMaximumC = daily.arrayDoubleAt("temperature_2m_max", 0),
            todayPrecipitationProbabilityPercent = daily.arrayIntAt("precipitation_probability_max", 0),
            sunrise = daily.arrayStringAt("sunrise", 0),
            sunset = daily.arrayStringAt("sunset", 0)
        )
    }

    private fun findNextHour(times: JSONArray?, currentTime: String): Int? {
        if (times == null || currentTime.isBlank()) return null
        return (0 until times.length()).firstOrNull { index ->
            times.optString(index).let { it.isNotBlank() && it >= currentTime }
        }
    }

    private fun getText(address: String): String {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Relogio-2.0-Android")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("Serviço meteorológico respondeu HTTP $status.")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (body.length > MAX_RESPONSE_CHARS) {
                throw IOException("Resposta meteorológica excedeu o limite permitido.")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.nullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return optDouble(key, Double.NaN).takeIf { it.isFinite() }
    }

    private fun JSONObject.nullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
    }

    private fun JSONArray.nullableIntAt(index: Int): Int? =
        optInt(index, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }

    private fun JSONObject.arrayDoubleAt(key: String, index: Int): Double? =
        optJSONArray(key)?.optDouble(index, Double.NaN)?.takeIf { it.isFinite() }

    private fun JSONObject.arrayIntAt(key: String, index: Int): Int? =
        optJSONArray(key)?.nullableIntAt(index)

    private fun JSONObject.arrayStringAt(key: String, index: Int): String? =
        optJSONArray(key)?.optString(index, "")?.takeIf { it.isNotBlank() }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val MAX_RESPONSE_CHARS = 512_000
    }
}
