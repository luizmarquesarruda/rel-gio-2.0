package com.luizmarquesarruda.relogio20.notifications

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationCaptureStoreTest {
    private val testElapsedRealtime = 10_000L

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
        NotificationCaptureStore.add(
            preview("android-key", "com.whatsapp"),
            nowElapsedRealtimeMillis = testElapsedRealtime
        )
        NotificationCaptureStore.remove("android-key")

        assertTrue(NotificationCaptureStore.snapshot(testElapsedRealtime).isEmpty())
    }

    @Test
    fun removingAnAppPurgesOnlyItsPreviews() {
        NotificationCaptureStore.add(
            preview("wa-1", "com.whatsapp"),
            nowElapsedRealtimeMillis = testElapsedRealtime
        )
        NotificationCaptureStore.add(
            preview("mail-1", "com.example.mail"),
            nowElapsedRealtimeMillis = testElapsedRealtime
        )

        NotificationCaptureStore.removePackage("com.whatsapp")

        val remaining = NotificationCaptureStore.snapshot(testElapsedRealtime)
        assertEquals(1, remaining.size)
        assertEquals("com.example.mail", remaining.single().packageName)
        assertFalse(remaining.any { it.packageName == "com.whatsapp" })
    }

    @Test
    fun retainsOnlyTheThirtyNewestPreviews() {
        repeat(35) { index ->
            NotificationCaptureStore.add(
                preview("key-$index", "com.example.app"),
                nowElapsedRealtimeMillis = testElapsedRealtime
            )
        }

        val remaining = NotificationCaptureStore.snapshot(testElapsedRealtime)
        assertEquals(30, remaining.size)
        assertEquals("key-34", remaining.first().key)
        assertEquals("key-5", remaining.last().key)
    }

    @Test
    fun expiresPreviewsAfterFifteenMinutes() {
        NotificationCaptureStore.add(
            preview("temporary", "com.example.app"),
            nowElapsedRealtimeMillis = testElapsedRealtime
        )

        val beforeExpiry = NotificationCaptureStore.snapshot(
            testElapsedRealtime + NotificationCaptureStore.RETENTION_MILLIS - 1
        )
        val afterExpiry = NotificationCaptureStore.snapshot(
            testElapsedRealtime + NotificationCaptureStore.RETENTION_MILLIS
        )

        assertEquals(1, beforeExpiry.size)
        assertTrue(afterExpiry.isEmpty())
    }

    private fun preview(key: String, packageName: String) = NotificationPreview(
        key = key,
        packageName = packageName,
        appLabel = packageName,
        title = "title",
        body = "body",
        receivedAtMillis = 1_000L,
        capturedAtElapsedRealtimeMillis = testElapsedRealtime
    )
}
