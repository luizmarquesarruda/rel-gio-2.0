package com.luizmarquesarruda.relogio20.device.discovery

internal enum class GattProgressOperation { READ, SUBSCRIBE }

internal enum class GattProgressState {
    QUEUED, RUNNING, RESPONDED, SUBSCRIBED, FAILED, TIMED_OUT, SKIPPED
}

internal data class GattChannelProgress(
    val serviceUuid: String,
    val characteristicUuid: String,
    val operation: GattProgressOperation,
    val state: GattProgressState,
    val attempts: Int,
    val bytesReceived: Int,
    val notifications: Int,
    val decodedValues: Int,
    val lastObservedAtMillis: Long?,
    val classification: String
)

internal data class GattExplorationProgressSnapshot(
    val total: Int,
    val completed: Int,
    val readsResponded: Int,
    val subscriptionsConfirmed: Int,
    val notifications: Int,
    val decodedValues: Int,
    val failures: Int,
    val timeouts: Int,
    val queued: Int,
    val running: Int,
    val channels: List<GattChannelProgress>
) {
    val completionPercent: Int
        get() = if (total == 0) 0 else (completed * 100 / total).coerceIn(0, 100)

    fun summary(): String =
        if (total == 0) {
            "Exploração GATT: aguardando início."
        } else {
            "Exploração GATT: $completionPercent% • $completed/$total operações concluídas\n" +
                "READ respondidos: $readsResponded • CCCD confirmados: $subscriptionsConfirmed\n" +
                "Notificações: $notifications • valores decodificados: $decodedValues\n" +
                "Falhas: $failures • timeouts: $timeouts • na fila: $queued • em execução: $running"
        }

    fun channelSummary(limit: Int = 5): String =
        channels.take(limit).joinToString("\n") {
            it.characteristicUuid + " — " + it.classification
        }
}

/** Tracks transport evidence only; a silent channel is never assumed to be a missing sensor. */
internal class GattExplorationProgressTracker {
    private data class MutableEntry(
        val serviceUuid: String,
        val characteristicUuid: String,
        val operation: GattProgressOperation,
        var state: GattProgressState = GattProgressState.QUEUED,
        var attempts: Int = 0,
        var bytesReceived: Int = 0,
        var notifications: Int = 0,
        var decodedValues: Int = 0,
        var lastObservedAtMillis: Long? = null
    )

    private val entries = linkedMapOf<String, MutableEntry>()

    fun begin(operations: List<SafeGattExplorationPlan.Operation>) {
        entries.clear()
        operations.forEach { operation ->
            val kind = when (operation.kind) {
                SafeGattExplorationPlan.Kind.READ_STANDARD_VALUE,
                SafeGattExplorationPlan.Kind.READ_PROPRIETARY_VALUE -> GattProgressOperation.READ
                SafeGattExplorationPlan.Kind.SUBSCRIBE_STANDARD_MEASUREMENT,
                SafeGattExplorationPlan.Kind.SUBSCRIBE_PROPRIETARY_CHANNEL -> GattProgressOperation.SUBSCRIBE
            }
            val entry = MutableEntry(
                normalize(operation.serviceUuid),
                normalize(operation.characteristicUuid),
                kind
            )
            entries[key(entry.serviceUuid, entry.characteristicUuid, kind)] = entry
        }
    }

    fun markStarted(serviceUuid: String, characteristicUuid: String, operation: GattProgressOperation) {
        find(serviceUuid, characteristicUuid, operation)?.let {
            if (it.state == GattProgressState.QUEUED) {
                it.state = GattProgressState.RUNNING
                it.attempts += 1
            }
        }
    }

    fun markReadResult(
        serviceUuid: String,
        characteristicUuid: String,
        success: Boolean,
        bytesReceived: Int,
        decodedValues: Int
    ) {
        find(serviceUuid, characteristicUuid, GattProgressOperation.READ)?.let {
            it.state = if (success) GattProgressState.RESPONDED else GattProgressState.FAILED
            it.bytesReceived = bytesReceived.coerceAtLeast(0)
            it.decodedValues = decodedValues.coerceAtLeast(0)
        }
    }

    fun markSubscriptionResult(serviceUuid: String, characteristicUuid: String, success: Boolean) {
        find(serviceUuid, characteristicUuid, GattProgressOperation.SUBSCRIBE)?.let {
            it.state = if (success) GattProgressState.SUBSCRIBED else GattProgressState.FAILED
        }
    }

    fun markTimeout(serviceUuid: String, characteristicUuid: String, operation: GattProgressOperation) {
        find(serviceUuid, characteristicUuid, operation)?.let { it.state = GattProgressState.TIMED_OUT }
    }

    fun markRejected(serviceUuid: String, characteristicUuid: String, operation: GattProgressOperation) {
        find(serviceUuid, characteristicUuid, operation)?.let { it.state = GattProgressState.FAILED }
    }

    fun markSkipped(serviceUuid: String, characteristicUuid: String, operation: GattProgressOperation) {
        find(serviceUuid, characteristicUuid, operation)?.let {
            if (it.state == GattProgressState.QUEUED || it.state == GattProgressState.RUNNING) {
                it.state = GattProgressState.SKIPPED
            }
        }
    }

    fun markRemainingSkipped() {
        entries.values.forEach {
            if (it.state == GattProgressState.QUEUED || it.state == GattProgressState.RUNNING) {
                it.state = GattProgressState.SKIPPED
            }
        }
    }

    fun observeNotification(
        serviceUuid: String,
        characteristicUuid: String,
        bytesReceived: Int,
        decodedValues: Int,
        observedAtMillis: Long
    ) {
        find(serviceUuid, characteristicUuid, GattProgressOperation.SUBSCRIBE)?.let {
            it.notifications += 1
            it.bytesReceived += bytesReceived.coerceAtLeast(0)
            it.decodedValues += decodedValues.coerceAtLeast(0)
            it.lastObservedAtMillis = observedAtMillis
        }
    }

    fun snapshot(): GattExplorationProgressSnapshot {
        val values = entries.values.toList()
        val terminal = setOf(
            GattProgressState.RESPONDED, GattProgressState.SUBSCRIBED,
            GattProgressState.FAILED, GattProgressState.TIMED_OUT, GattProgressState.SKIPPED
        )
        val channels = values.map { entry ->
            val classification = when {
                entry.decodedValues > 0 -> "DADOS DECODIFICADOS"
                entry.notifications > 0 || entry.bytesReceived > 0 ->
                    "BYTES OBSERVADOS • SEM SEMÂNTICA CONFIRMADA"
                entry.state == GattProgressState.SUBSCRIBED -> "INSCRITO • AGUARDANDO DADOS"
                entry.state == GattProgressState.RESPONDED -> "READ RESPONDEU • SEM VALOR DECODIFICADO"
                entry.state == GattProgressState.TIMED_OUT -> "TIMEOUT"
                entry.state == GattProgressState.FAILED -> "FALHA"
                entry.state == GattProgressState.SKIPPED -> "NÃO EXECUTADO"
                entry.state == GattProgressState.RUNNING -> "EM EXECUÇÃO"
                else -> "NA FILA"
            }
            GattChannelProgress(
                entry.serviceUuid, entry.characteristicUuid, entry.operation, entry.state,
                entry.attempts, entry.bytesReceived, entry.notifications, entry.decodedValues,
                entry.lastObservedAtMillis, classification
            )
        }
        return GattExplorationProgressSnapshot(
            total = values.size,
            completed = values.count { it.state in terminal },
            readsResponded = values.count {
                it.operation == GattProgressOperation.READ && it.state == GattProgressState.RESPONDED
            },
            subscriptionsConfirmed = values.count {
                it.operation == GattProgressOperation.SUBSCRIBE && it.state == GattProgressState.SUBSCRIBED
            },
            notifications = values.sumOf { it.notifications },
            decodedValues = values.sumOf { it.decodedValues },
            failures = values.count { it.state == GattProgressState.FAILED },
            timeouts = values.count { it.state == GattProgressState.TIMED_OUT },
            queued = values.count { it.state == GattProgressState.QUEUED },
            running = values.count { it.state == GattProgressState.RUNNING },
            channels = channels
        )
    }

    private fun find(
        serviceUuid: String,
        characteristicUuid: String,
        operation: GattProgressOperation
    ): MutableEntry? = entries[key(serviceUuid, characteristicUuid, operation)]

    private fun key(serviceUuid: String, characteristicUuid: String, operation: GattProgressOperation): String =
        normalize(serviceUuid) + "/" + normalize(characteristicUuid) + "/" + operation.name

    private fun normalize(value: String): String = value.trim().lowercase()
}
