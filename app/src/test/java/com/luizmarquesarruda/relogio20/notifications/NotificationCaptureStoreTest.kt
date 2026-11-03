package com.luizmarquesarruda.relogio20.notifications

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationCaptureStoreTest {
    @Before
    fun clearBefore() {
        NotificationCaptureStore.clear()
    }

    @After
    fun clearAfter() {
        NotificationCaptureStore.clear()
    }

    @Test
    fun removesNotificationByAndroidKey() {
        NotificationCaptureStore.add(preview("android-key", "com.whatsapp"))
        NotificationCaptureStore.remove("android-key")

        assertTrue(NotificationCaptureStore.snapshot().isEmpty())
    }

    @Test
    fun removingAnAppPurgesOnlyItsPreviews() {
        NotificationCaptureStore.add(preview("wa-1", "com.whatsapp"))
        NotificationCaptureStore.add(preview("mail-1", "com.example.mail"))

        NotificationCaptureStore.removePackage("com.whatsapp")

        val remaining = NotificationCaptureStore.snapshot()
        assertEquals(1, remaining.size)
        assertEquals("com.example.mail", remaining.single().packageName)
        assertFalse(remaining.any { it.packageName == "com.whatsapp" })
    }

    @Test
    fun retainsOnlyTheThirtyNewestPreviews() {
        repeat(35) { index ->
            NotificationCaptureStore.add(preview("key-$index", "com.example.app"))
        }

        val remaining = NotificationCaptureStore.snapshot()
        assertEquals(30, remaining.size)
        assertEquals("key-34", remaining.first().key)
        assertEquals("key-5", remaining.last().key)
    }

    private fun preview(key: String, packageName: String) = NotificationPreview(
        key = key,
        packageName = packageName,
        appLabel = packageName,
        title = "title",
        body = "body",
        receivedAtMillis = 1L
    )
}
