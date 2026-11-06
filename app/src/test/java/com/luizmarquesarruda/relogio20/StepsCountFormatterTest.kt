package com.luizmarquesarruda.relogio20

import com.luizmarquesarruda.relogio20.ui.formatters.StepsCountFormatter

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class StepsCountFormatterTest {
    @Test
    fun missingAggregateIsNotPresentedAsZero() {
        assertEquals(
            "Passos hoje: sem registros",
            StepsCountFormatter.label(null, Locale.US)
        )
    }

    @Test
    fun validZeroRemainsARealZero() {
        assertEquals("Passos hoje: 0", StepsCountFormatter.label(0L, Locale.US))
    }

    @Test
    fun formatsPositiveCountsWithGrouping() {
        assertEquals("Passos hoje: 12,345", StepsCountFormatter.label(12_345L, Locale.US))
    }
}
