package com.luizmarquesarruda.relogio20.device.ble.gatt

data class GattDescriptorInventoryRow(
    val characteristicUuid: String,
    val descriptorUuid: String,
    val instanceId: Int,
)

internal object GattInventoryLogger {
    fun deduplicate(rows: List<GattDescriptorInventoryRow>): List<GattDescriptorInventoryRow> =
        rows.distinctBy {
            Triple(
                it.characteristicUuid.trim().lowercase(),
                it.descriptorUuid.trim().lowercase(),
                it.instanceId,
            )
        }
}
