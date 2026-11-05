package com.luizmarquesarruda.relogio20.domain.health

import java.util.Locale

internal object StepsCountFormatter {
    fun label(count: Long?, locale: Locale = Locale.getDefault()): String {
        if (count == null) return "Passos hoje: sem registros"
        return "Passos hoje: " + String.format(locale, "%,d", count)
    }
}
