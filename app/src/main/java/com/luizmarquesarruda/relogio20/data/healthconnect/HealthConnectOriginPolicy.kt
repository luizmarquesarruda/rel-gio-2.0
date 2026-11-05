package com.luizmarquesarruda.relogio20.data.healthconnect

import androidx.health.connect.client.records.metadata.DataOrigin

internal enum class HealthConnectOriginPolicy { APP_ONLY, INCLUDE_EXTERNAL }

internal fun HealthConnectOriginPolicy.dataOriginFilter(appPackage: String): Set<DataOrigin> {
    require(appPackage.isNotBlank())
    return when (this) {
        HealthConnectOriginPolicy.APP_ONLY -> setOf(DataOrigin(appPackage))
        HealthConnectOriginPolicy.INCLUDE_EXTERNAL -> emptySet()
    }
}
