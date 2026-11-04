package com.luizmarquesarruda.relogio20.device.discovery

/**
 * Session-scoped, metadata-only observer for BLE notification traffic.
 * It never stores payload bytes; callers provide a precomputed fingerprint.
 */
internal class PassiveBleObservationTracker(
    private val maxChannels: Int = 32,
    private val maxFingerprintsPerChannel: Int = 32
) {
    data class Observation(
        val sequence: Long,
        val channelNotificationCount: Long,
        val channelByteCount: Long,
        val intervalSincePreviousMillis: Long?,
        val distinctFingerprints: Int
    )

    private data class ChannelState(
        var notificationCount: Long = 0,
        var byteCount: Long = 0,
        var lastObservedAtMillis: Long? = null,
        val fingerprints: LinkedHashSet<String> = linkedSetOf()
    )

    private val channels = linkedMapOf<String, ChannelState>()
    private var sequence = 0L

    init {
        require(maxChannels > 0)
        require(maxFingerprintsPerChannel > 0)
    }

    @Synchronized
    fun observe(
        serviceUuid: String,
        characteristicUuid: String,
        payloadBytes: Int,
        fingerprint: String,
        observedAtMillis: Long
    ): Observation {
        require(payloadBytes >= 0)
        require(observedAtMillis >= 0)
        val key = serviceUuid.lowercase() + "/" + characteristicUuid.lowercase()
        val state = channels.getOrPut(key) { ChannelState() }
        while (channels.size > maxChannels) channels.remove(channels.keys.first())

        val previousAt = state.lastObservedAtMillis
        val interval = previousAt?.let { (observedAtMillis - it).coerceAtLeast(0L) }
        state.notificationCount += 1
        state.byteCount += payloadBytes.toLong()
        state.lastObservedAtMillis = observedAtMillis
        if (fingerprint.isNotBlank()) {
            state.fingerprints.add(fingerprint)
            while (state.fingerprints.size > maxFingerprintsPerChannel) {
                state.fingerprints.remove(state.fingerprints.first())
            }
        }
        sequence += 1

        return Observation(
            sequence = sequence,
            channelNotificationCount = state.notificationCount,
            channelByteCount = state.byteCount,
            intervalSincePreviousMillis = interval,
            distinctFingerprints = state.fingerprints.size
        )
    }

    @Synchronized
    fun resetSession() {
        channels.clear()
        sequence = 0L
    }
}
