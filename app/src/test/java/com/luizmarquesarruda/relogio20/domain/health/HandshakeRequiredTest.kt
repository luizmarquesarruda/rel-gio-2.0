package com.luizmarquesarruda.relogio20.domain.health

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class HandshakeRequiredTest {
    @Test
    fun discoveryOnlyNeverEmitsMetrics() = runBlocking {
        val source = DiscoveryOnlySource()
        val handle = source.initiateSession().getOrThrow()
        assertTrue(handle is SessionHandle.DiscoveryOnly)
        assertTrue(source.observeMetrics(handle).toList().isEmpty())
    }
}
