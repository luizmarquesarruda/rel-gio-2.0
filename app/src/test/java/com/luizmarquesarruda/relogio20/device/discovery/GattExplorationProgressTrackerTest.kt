package com.luizmarquesarruda.relogio20.device.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GattExplorationProgressTrackerTest {
    @Test
    fun summarizesReadSubscriptionNotificationsAndDecodedValues() {
        val read =
            SafeGattExplorationPlan.Operation(
                "0000180f-0000-1000-8000-00805f9b34fb",
                "00002a19-0000-1000-8000-00805f9b34fb",
                SafeGattExplorationPlan.Kind.READ_STANDARD_VALUE,
                "battery",
            )
        val notify =
            SafeGattExplorationPlan.Operation(
                "00003802-0000-1000-8000-00805f9b34fb",
                "00004a02-0000-1000-8000-00805f9b34fb",
                SafeGattExplorationPlan.Kind.SUBSCRIBE_PROPRIETARY_CHANNEL,
                "custom notify",
            )
        val tracker = GattExplorationProgressTracker()
        tracker.begin(listOf(read, notify))

        tracker.markStarted(read.serviceUuid, read.characteristicUuid, GattProgressOperation.READ)
        tracker.markReadResult(read.serviceUuid, read.characteristicUuid, true, 1, 1)
        tracker.markStarted(notify.serviceUuid, notify.characteristicUuid, GattProgressOperation.SUBSCRIBE)
        tracker.markSubscriptionResult(notify.serviceUuid, notify.characteristicUuid, true)
        tracker.observeNotification(notify.serviceUuid, notify.characteristicUuid, 4, 0, 10_000L)

        val snapshot = tracker.snapshot()
        assertEquals(2, snapshot.total)
        assertEquals(2, snapshot.completed)
        assertEquals(0, snapshot.skipped)
        assertEquals(1, snapshot.readsResponded)
        assertEquals(1, snapshot.subscriptionsConfirmed)
        assertEquals(1, snapshot.notifications)
        assertEquals(1, snapshot.decodedValues)
        assertEquals(100, snapshot.completionPercent)
        assertTrue(snapshot.channels.any { it.classification == "DADOS DECODIFICADOS" })
        assertTrue(snapshot.channels.any { it.classification.contains("BYTES OBSERVADOS") })
    }

    @Test
    fun distinguishesSubscribedButSilentFromTimedOutAndFailedChannels() {
        val ops =
            listOf(
                SafeGattExplorationPlan.Operation("180d", "2a37", SafeGattExplorationPlan.Kind.SUBSCRIBE_STANDARD_MEASUREMENT, "heart"),
                SafeGattExplorationPlan.Operation("1810", "2a35", SafeGattExplorationPlan.Kind.READ_STANDARD_VALUE, "pressure"),
                SafeGattExplorationPlan.Operation("181a", "2a6e", SafeGattExplorationPlan.Kind.READ_STANDARD_VALUE, "temperature"),
            )
        val tracker = GattExplorationProgressTracker()
        tracker.begin(ops)
        tracker.markSubscriptionResult("180d", "2a37", true)
        tracker.markTimeout("1810", "2a35", GattProgressOperation.READ)
        tracker.markRejected("181a", "2a6e", GattProgressOperation.READ)

        val snapshot = tracker.snapshot()
        assertEquals(1, snapshot.subscriptionsConfirmed)
        assertEquals(1, snapshot.timeouts)
        assertEquals(1, snapshot.failures)
        assertTrue(snapshot.channels.any { it.classification == "INSCRITO • AGUARDANDO DADOS" })
        assertTrue(snapshot.channels.any { it.classification == "TIMEOUT" })
        assertTrue(snapshot.channels.any { it.classification == "FALHA" })
    }

    @Test
    fun disconnectMarksQueuedOperationsAsNotExecuted() {
        val operation =
            SafeGattExplorationPlan.Operation(
                "181b",
                "2a9c",
                SafeGattExplorationPlan.Kind.SUBSCRIBE_STANDARD_MEASUREMENT,
                "body composition",
            )
        val tracker = GattExplorationProgressTracker()
        tracker.begin(listOf(operation))
        tracker.markRemainingSkipped()

        val channel = tracker.snapshot().channels.single()
        assertEquals(GattProgressState.SKIPPED, channel.state)
        assertEquals(0, tracker.snapshot().completed)
        assertEquals(1, tracker.snapshot().skipped)
        assertEquals(100, tracker.snapshot().completionPercent)
        assertEquals("NÃO EXECUTADO", channel.classification)
    }
}
