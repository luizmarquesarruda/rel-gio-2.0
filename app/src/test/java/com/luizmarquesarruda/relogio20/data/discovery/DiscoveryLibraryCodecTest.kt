package com.luizmarquesarruda.relogio20.data.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class DiscoveryLibraryCodecTest {
    @Test
    fun roundTripsSessionAndReadingMetadata() {
        val snapshot = DiscoveryLibrarySnapshot(
            activeSession = DiscoveryTestSession("session-1", 1_000L, 121_000L, 120),
            records = listOf(
                DiscoveryLibraryRecord(
                    id = "record-1",
                    occurredAtMillis = 2_000L,
                    sessionId = "session-1",
                    kind = DiscoveryLibraryKind.NOTIFICATION,
                    title = "Notificação BLE",
                    detail = "8 bytes recebidos",
                    source = "BLE_NOTIFY",
                    confidence = "OBSERVADO",
                    serviceUuid = "service",
                    characteristicUuid = "characteristic",
                    payloadHex = "AB0005FF91800032"
                )
            )
        )

        val decoded = DiscoveryLibraryCodec.decode(DiscoveryLibraryCodec.encode(snapshot))
        assertTrue(decoded is DiscoveryLibraryReadResult.Available)
        val actual = (decoded as DiscoveryLibraryReadResult.Available).snapshot
        assertEquals(snapshot.activeSession, actual.activeSession)
        assertEquals(snapshot.records, actual.records)
    }

    @Test
    fun rejectsCorruptMagicWithoutProducingAnEmptyLibrary() {
        val decoded = DiscoveryLibraryCodec.decode(byteArrayOf(0, 1, 2, 3, 4, 5))
        assertTrue(decoded is DiscoveryLibraryReadResult.Corrupt)
    }

    @Test
    fun refusesFutureSchemaInsteadOfTreatingItAsEmpty() {
        val bytes = ByteArrayOutputStream().also { output ->
            DataOutputStream(output).use { data ->
                data.writeInt(DiscoveryLibraryCodec.MAGIC)
                data.writeInt(DiscoveryLibraryCodec.SCHEMA_VERSION + 1)
            }
        }.toByteArray()
        val decoded = DiscoveryLibraryCodec.decode(bytes)
        assertTrue(decoded is DiscoveryLibraryReadResult.UnsupportedSchema)
    }
}
