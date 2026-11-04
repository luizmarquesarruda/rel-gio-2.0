package com.luizmarquesarruda.relogio20.device.discovery

/** Gates the unattended GATT probe behind explicit local consent and a full device fingerprint. */
internal object AutomaticGattDiscoveryPolicy {
    fun shouldStart(
        consentEnabled: Boolean,
        fullDeviceFingerprintMatched: Boolean,
        alreadyStartedForConnection: Boolean
    ): Boolean =
        consentEnabled && fullDeviceFingerprintMatched && !alreadyStartedForConnection
}
