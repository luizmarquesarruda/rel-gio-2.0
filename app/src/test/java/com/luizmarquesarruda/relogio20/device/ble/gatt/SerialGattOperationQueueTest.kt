package com.luizmarquesarruda.relogio20.device.ble.gatt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SerialGattOperationQueueTest {
    @Test
    fun startsOnlyOneOperationUntilCallbackCompletesIt() {
        val queue = SerialGattOperationQueue()
        val started = mutableListOf<Int>()

        queue.enqueue {
            started.add(1)
            true
        }
        queue.enqueue {
            started.add(2)
            true
        }

        assertEquals(listOf(1), started)
        assertTrue(queue.isOperationInProgress)
        assertEquals(1, queue.pendingOperationCount)

        queue.onOperationCompleted()

        assertEquals(listOf(1, 2), started)
        assertTrue(queue.isOperationInProgress)

        queue.onOperationCompleted()
        assertFalse(queue.isOperationInProgress)
        assertEquals(0, queue.pendingOperationCount)
    }

    @Test
    fun skipsAnOperationThatAndroidCouldNotStart() {
        val queue = SerialGattOperationQueue()
        val started = mutableListOf<Int>()

        queue.enqueue {
            started.add(1)
            false
        }
        queue.enqueue {
            started.add(2)
            true
        }

        assertEquals(listOf(1, 2), started)
        assertTrue(queue.isOperationInProgress)
    }

    @Test
    fun resetDropsPendingOperationsAndReleasesBusyState() {
        val queue = SerialGattOperationQueue()
        var secondStarted = false

        queue.enqueue { true }
        queue.enqueue {
            secondStarted = true
            true
        }

        queue.reset()

        assertFalse(queue.isOperationInProgress)
        assertEquals(0, queue.pendingOperationCount)
        assertFalse(secondStarted)
    }
}
