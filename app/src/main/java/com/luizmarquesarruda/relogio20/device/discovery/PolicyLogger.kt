package com.luizmarquesarruda.relogio20.device.discovery

internal object PolicyLogger {
    fun serialize(policy: ExplorationPolicy): String = policy.serialize()
}
