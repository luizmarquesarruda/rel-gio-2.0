package com.luizmarquesarruda.relogio20.data.weather

internal data class WeatherLocation(
    val name: String,
    val admin1: String?,
    val country: String,
    val latitude: Double,
    val longitude: Double
) {
    val displayName: String
        get() = listOfNotNull(name, admin1, country.takeIf { it.isNotBlank() })
            .distinct()
            .joinToString(" · ")
}

internal data class WeatherSnapshot(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
    val observedAt: String,
    val fetchedAtEpochMillis: Long,
    val temperatureC: Double?,
    val apparentTemperatureC: Double?,
    val humidityPercent: Int?,
    val precipitationMm: Double?,
    val precipitationProbabilityNextHourPercent: Int?,
    val weatherCode: Int?,
    val windSpeedKmh: Double?,
    val windDirectionDegrees: Int?,
    val windGustsKmh: Double?,
    val todayMinimumC: Double?,
    val todayMaximumC: Double?,
    val todayPrecipitationProbabilityPercent: Int?,
    val sunrise: String?,
    val sunset: String?
)

internal object WeatherCodeLabels {
    fun describe(code: Int?): String = when (code) {
        0 -> "Céu limpo"
        1 -> "Predominantemente limpo"
        2 -> "Parcialmente nublado"
        3 -> "Encoberto"
        45, 48 -> "Nevoeiro"
        51 -> "Garoa fraca"
        53 -> "Garoa moderada"
        55 -> "Garoa intensa"
        56, 57 -> "Garoa congelante"
        61 -> "Chuva fraca"
        63 -> "Chuva moderada"
        65 -> "Chuva intensa"
        66, 67 -> "Chuva congelante"
        71 -> "Neve fraca"
        73 -> "Neve moderada"
        75 -> "Neve intensa"
        77 -> "Grãos de neve"
        80 -> "Pancadas de chuva fracas"
        81 -> "Pancadas de chuva moderadas"
        82 -> "Pancadas de chuva fortes"
        85, 86 -> "Pancadas de neve"
        95 -> "Trovoada"
        96, 99 -> "Trovoada com granizo"
        null -> "Condição indisponível"
        else -> "Condição meteorológica não classificada"
    }
}
