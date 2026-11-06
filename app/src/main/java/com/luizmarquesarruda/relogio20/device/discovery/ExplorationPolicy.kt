package com.luizmarquesarruda.relogio20.device.discovery

import java.util.Locale

enum class PolicySource { BUILD_CONFIG, REMOTE_CONFIG, USER_OVERRIDE }

internal data class ExplorationPolicy(
    val source: PolicySource,
    val allowlist: Set<String>,
    val blocklist: Set<String>,
    val allowReads: Boolean,
    val allowWrites: Boolean
) {
    enum class Operation { READ, WRITE }

    fun isEligible(serviceUuid: String, characteristicUuid: String, operation: Operation): Boolean {
        val service = normalize(serviceUuid)
        val characteristic = normalize(characteristicUuid)
        val key = "$service/$characteristic"
        if (service in HARD_BLOCKLIST || characteristic in HARD_BLOCKLIST ||
            "service:$service" in blocklist || "characteristic:$characteristic" in blocklist ||
            key in blocklist || key !in allowlist
        ) return false

        return when (operation) {
            Operation.READ -> allowReads
            Operation.WRITE -> allowWrites &&
                service == CTS_SERVICE && characteristic == CTS_CURRENT_TIME
        }
    }

    fun serialize(): String = buildString {
        append("source=").append(source.name)
        append(";allowReads=").append(allowReads)
        append(";allowWrites=").append(allowWrites)
        append(";allowlist=").append(allowlist.map(::normalize).sorted().joinToString(","))
        append(";blocklist=").append(
            (blocklist + HARD_BLOCKLIST.map { "characteristic:$it" } +
                "service:$DFU_SERVICE" + "service:$NUS_SERVICE")
                .map(::normalize).distinct().sorted().joinToString(",")
        )
    }

    companion object {
        private const val DFU_SERVICE = "0000fe59-0000-1000-8000-00805f9b34fb"
        private const val DFU_CONTROL_POINT = "8ec90001-f315-4f60-9fb8-838830daea50"
        private const val DFU_PACKET = "8ec90002-f315-4f60-9fb8-838830daea50"
        private const val NUS_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
        private const val NUS_RX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
        private const val CTS_SERVICE = "00001805-0000-1000-8000-00805f9b34fb"
        private const val CTS_CURRENT_TIME = "00002a2b-0000-1000-8000-00805f9b34fb"
        private val HARD_BLOCKLIST = setOf(DFU_SERVICE, DFU_CONTROL_POINT, DFU_PACKET, NUS_SERVICE, NUS_RX)

        fun forObservedInventory(
            services: List<GattServiceRecord>,
            source: PolicySource = PolicySource.BUILD_CONFIG
        ): ExplorationPolicy {
            val allow = services.flatMap { service ->
                val id = normalize(service.uuid)
                if (id == DFU_SERVICE || id == NUS_SERVICE ||
                    id == "00001800-0000-1000-8000-00805f9b34fb" ||
                    id == "00001801-0000-1000-8000-00805f9b34fb"
                ) emptyList() else service.characteristics
                    .filter { characteristic ->
                        val characteristicId = normalize(characteristic.uuid)
                        characteristicId !in HARD_BLOCKLIST &&
                            characteristicId !in setOf(
                                "00002a23-0000-1000-8000-00805f9b34fb",
                                "00002a25-0000-1000-8000-00805f9b34fb",
                                "00002a50-0000-1000-8000-00805f9b34fb"
                            ) &&
                            characteristic.properties.any { it == "READ" || it == "NOTIFY" || it == "INDICATE" }
                    }
                    .map { "$id/${normalize(it.uuid)}" }
            }.toSet()

            return ExplorationPolicy(
                source,
                allow,
                HARD_BLOCKLIST.map { "characteristic:$it" }.toSet() +
                    "service:$DFU_SERVICE" + "service:$NUS_SERVICE",
                allowReads = true,
                allowWrites = false
            )
        }

        fun failClosed(source: PolicySource = PolicySource.BUILD_CONFIG) =
            ExplorationPolicy(
                source,
                emptySet(),
                HARD_BLOCKLIST.map { "characteristic:$it" }.toSet() +
                    "service:$DFU_SERVICE" + "service:$NUS_SERVICE",
                allowReads = false,
                allowWrites = false
            )

        private fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT)
    }
}
