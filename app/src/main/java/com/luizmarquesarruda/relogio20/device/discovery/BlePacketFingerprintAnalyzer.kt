package com.luizmarquesarruda.relogio20.device.discovery

import java.security.MessageDigest

/** Metadata-only analyzer. Payload bytes are never retained by this class. */
internal object BlePacketFingerprintAnalyzer {
    data class Fingerprint(
        val length: Int,
        val prefixHex: String,
        val sha256Prefix: String,
        val firstByte: Int?,
        val lastByte: Int?,
        val repeatedCount: Int,
    )

    private val observations = linkedMapOf<String, Int>()

    @Synchronized
    fun observe(
        serviceUuid: String,
        characteristicUuid: String,
        payload: ByteArray,
    ): Fingerprint {
        val prefix =
            payload
                .take(4)
                .joinToString("") {
                    (it.toInt() and 0xff).toString(16).padStart(2, '0')
                }.uppercase()
        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(payload)
                .take(6)
                .joinToString("") {
                    (it.toInt() and 0xff).toString(16).padStart(2, '0')
                }
        val key =
            serviceUuid.lowercase() + "/" + characteristicUuid.lowercase() +
                "/" + payload.size + "/" + digest
        val count = (observations[key] ?: 0) + 1
        observations[key] = count
        while (observations.size > 256) observations.remove(observations.keys.first())
        return Fingerprint(
            length = payload.size,
            prefixHex = prefix,
            sha256Prefix = digest.uppercase(),
            firstByte = payload.firstOrNull()?.toInt()?.and(0xff),
            lastByte = payload.lastOrNull()?.toInt()?.and(0xff),
            repeatedCount = count,
        )
    }

    @Synchronized
    fun resetSession() = observations.clear()
}
