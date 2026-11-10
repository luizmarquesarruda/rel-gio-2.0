package com.luizmarquesarruda.relogio20.device.discovery

/** Prevents automatic mode from terminating a session it only reused. */
internal object AutomaticCaptureSessionPolicy {
    fun shouldFinish(
        sessionMatches: Boolean,
        createdByAutomaticMode: Boolean,
    ): Boolean = sessionMatches && createdByAutomaticMode
}
