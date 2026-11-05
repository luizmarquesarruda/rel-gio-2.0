package com.luizmarquesarruda.relogio20.device.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmptyInventoryTest {
    @Test
    fun repeatedEmptyCallbacksAreBoundedAndValidInventoryResetsRetryBudget() {
        val policy = EmptyInventoryRetryPolicy(maxAttempts = 3)
        val firstTenEvents = (1..10).map { policy.nextRetry() }
        assertEquals(listOf(1, 2, 3, null, null, null, null, null, null, null), firstTenEvents)
        assertEquals(3, policy.attemptsUsed())

        // A non-empty inventory is the success boundary that starts a fresh budget.
        policy.reset()
        assertEquals(1, policy.nextRetry())
    }
}
