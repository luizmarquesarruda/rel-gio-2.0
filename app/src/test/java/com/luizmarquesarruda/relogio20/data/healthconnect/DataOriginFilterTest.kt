package com.luizmarquesarruda.relogio20.data.healthconnect

import org.junit.Assert.assertEquals
import org.junit.Test

class DataOriginFilterTest {
    @Test
    fun appOnlyFiltersAggregateToOurPackage() {
        val filter = HealthConnectOriginPolicy.APP_ONLY.dataOriginFilter("com.luizmarquesarruda.relogio20")
        assertEquals(setOf("com.luizmarquesarruda.relogio20"), filter.map { it.packageName }.toSet())
    }

    @Test
    fun externalInclusionMustBeExplicit() {
        val filter = HealthConnectOriginPolicy.INCLUDE_EXTERNAL.dataOriginFilter("com.luizmarquesarruda.relogio20")
        assertEquals(emptySet<androidx.health.connect.client.records.metadata.DataOrigin>(), filter)
    }
}
