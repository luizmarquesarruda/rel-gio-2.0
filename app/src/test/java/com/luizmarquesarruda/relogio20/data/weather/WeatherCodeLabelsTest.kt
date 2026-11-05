package com.luizmarquesarruda.relogio20.data.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeLabelsTest {
    @Test
    fun mapsClearCloudAndRainCodes() {
        assertEquals("Céu limpo", WeatherCodeLabels.describe(0))
        assertEquals("Parcialmente nublado", WeatherCodeLabels.describe(2))
        assertEquals("Chuva moderada", WeatherCodeLabels.describe(63))
    }

    @Test
    fun mapsStormAndFogCodes() {
        assertEquals("Trovoada", WeatherCodeLabels.describe(95))
        assertEquals("Nevoeiro", WeatherCodeLabels.describe(45))
    }

    @Test
    fun unknownOrMissingCodesRemainExplicit() {
        assertEquals("Condição indisponível", WeatherCodeLabels.describe(null))
        assertEquals("Condição meteorológica não classificada", WeatherCodeLabels.describe(1234))
    }
}
