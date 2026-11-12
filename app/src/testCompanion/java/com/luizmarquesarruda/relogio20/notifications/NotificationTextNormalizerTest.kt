package com.luizmarquesarruda.relogio20.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTextNormalizerTest {
    @Test
    fun collapsesWhitespaceAndTrimsText() {
        assertEquals(
            "Nova mensagem de teste",
            NotificationTextNormalizer.normalize("  Nova\n\t mensagem   de teste  "),
        )
    }

    @Test
    fun handlesNullAndBlankInput() {
        assertEquals("", NotificationTextNormalizer.normalize(null))
        assertEquals("", NotificationTextNormalizer.normalize("   "))
    }

    @Test
    fun limitsPreviewLength() {
        val value = "x".repeat(800)
        assertEquals(500, NotificationTextNormalizer.normalize(value).length)
    }
}
