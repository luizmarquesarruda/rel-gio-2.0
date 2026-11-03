package com.luizmarquesarruda.relogio20

import java.util.ArrayDeque

/**
 * Serializa operações GATT assíncronas.
 *
 * A operação retorna true quando a API Android aceitou iniciá-la.
 * O próximo item só é iniciado após onOperationCompleted().
 */
internal class SerialGattOperationQueue {
    private val pending = ArrayDeque<() -> Boolean>()
    var isOperationInProgress: Boolean = false
        private set

    val pendingOperationCount: Int
        get() = pending.size

    fun enqueue(operation: () -> Boolean) {
        pending.addLast(operation)
        startNext()
    }

    fun onOperationCompleted() {
        if (!isOperationInProgress) return
        isOperationInProgress = false
        startNext()
    }

    fun reset() {
        pending.clear()
        isOperationInProgress = false
    }

    private fun startNext() {
        if (isOperationInProgress) return

        while (pending.isNotEmpty()) {
            val operation = pending.removeFirst()
            val started = runCatching(operation).getOrDefault(false)
            if (started) {
                isOperationInProgress = true
                return
            }
        }
    }
}
