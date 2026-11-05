package com.luizmarquesarruda.relogio20.device.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeGattExplorationPlanTest {
    @Test
    fun automaticPlanReadsOnlyBasicDeviceMetadataAndBattery() {
        val plan = SafeGattExplorationPlan.build(
            listOf(
                GattServiceRecord(
                    "0000180f-0000-1000-8000-00805f9b34fb",
                    listOf(GattCharacteristicRecord(
                        "00002a19-0000-1000-8000-00805f9b34fb",
                        setOf("READ", "NOTIFY"),
                        setOf("00002902-0000-1000-8000-00805f9b34fb")
                    ))
                ),
                GattServiceRecord(
                    "0000180d-0000-1000-8000-00805f9b34fb",
                    listOf(GattCharacteristicRecord(
                        "00002a37-0000-1000-8000-00805f9b34fb",
                        setOf("NOTIFY"),
                        setOf("00002902-0000-1000-8000-00805f9b34fb")
                    ))
                )
            )
        )
        assertEquals(1, plan.size)
        assertEquals(SafeGattExplorationPlan.Kind.READ_STANDARD_VALUE, plan.single().kind)
        assertFalse(plan.any { it.kind.name.contains("SUBSCRIBE") })
    }

    @Test
    fun manualPlanIncludesStandardHealthNotificationsOnlyAfterUserAction() {
        val plan = SafeGattExplorationPlan.buildDeep(
            listOf(
                GattServiceRecord(
                    "0000180d-0000-1000-8000-00805f9b34fb",
                    listOf(GattCharacteristicRecord(
                        "00002a37-0000-1000-8000-00805f9b34fb",
                        setOf("NOTIFY"),
                        setOf("00002902-0000-1000-8000-00805f9b34fb")
                    ))
                )
            )
        )
        assertEquals(1, plan.size)
        assertEquals(
            SafeGattExplorationPlan.Kind.SUBSCRIBE_STANDARD_MEASUREMENT,
            plan.single().kind
        )
    }

    @Test
    fun deepPlanIncludesCustomReadAndNotifyButNeverCharacteristicWrites() {
        val plan = SafeGattExplorationPlan.buildDeep(
            listOf(
                GattServiceRecord(
                    "00003802-0000-1000-8000-00805f9b34fb",
                    listOf(GattCharacteristicRecord(
                        "00004a02-0000-1000-8000-00805f9b34fb",
                        setOf("READ", "NOTIFY", "WRITE"),
                        setOf("00002902-0000-1000-8000-00805f9b34fb")
                    ))
                )
            )
        )
        assertEquals(2, plan.size)
        assertTrue(plan.any { it.kind == SafeGattExplorationPlan.Kind.READ_PROPRIETARY_VALUE })
        assertTrue(plan.any { it.kind == SafeGattExplorationPlan.Kind.SUBSCRIBE_PROPRIETARY_CHANNEL })
        assertTrue(plan.none { it.kind.name.contains("WRITE") })
    }

    @Test
    fun deepPlanExcludesDfuNusChannelsAndDeviceIdentifiers() {
        val plan = SafeGattExplorationPlan.buildDeep(
            listOf(
                GattServiceRecord(
                    "0000fe59-0000-1000-8000-00805f9b34fb",
                    listOf(GattCharacteristicRecord(
                        "8ec90001-f315-4f60-9fb8-838830daea50",
                        setOf("READ", "NOTIFY", "WRITE"),
                        setOf("00002902-0000-1000-8000-00805f9b34fb")
                    ))
                ),
                GattServiceRecord(
                    "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
                    listOf(
                        GattCharacteristicRecord(
                            "6e400002-b5a3-f393-e0a9-e50e24dcca9e",
                            setOf("WRITE_NO_RESPONSE")
                        ),
                        GattCharacteristicRecord(
                            "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
                            setOf("NOTIFY"),
                            setOf("00002902-0000-1000-8000-00805f9b34fb")
                        )
                    )
                ),
                GattServiceRecord(
                    "0000180a-0000-1000-8000-00805f9b34fb",
                    listOf(GattCharacteristicRecord(
                        "00002a25-0000-1000-8000-00805f9b34fb",
                        setOf("READ")
                    ))
                )
            )
        )
        // The exact standard NUS TX receive channel is the only eligible NUS operation.
        assertEquals(1, plan.size)
        assertEquals(
            "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
            plan.single().characteristicUuid
        )
        assertEquals(
            SafeGattExplorationPlan.Kind.SUBSCRIBE_PROPRIETARY_CHANNEL,
            plan.single().kind
        )
        assertFalse(plan.any {
            it.characteristicUuid == "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
        })
        assertFalse(plan.any {
            it.kind == SafeGattExplorationPlan.Kind.READ_PROPRIETARY_VALUE ||
                it.kind.name.contains("WRITE")
        })
    }

    @Test
    fun deepPlanSubscribesOnlyExactNusTxReceiveChannel() {
        val plan = SafeGattExplorationPlan.buildDeep(
            listOf(
                GattServiceRecord(
                    "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
                    listOf(
                        GattCharacteristicRecord(
                            "6e400002-b5a3-f393-e0a9-e50e24dcca9e",
                            setOf("WRITE", "WRITE_NO_RESPONSE")
                        ),
                        GattCharacteristicRecord(
                            "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
                            setOf("READ", "NOTIFY", "WRITE"),
                            setOf("00002902-0000-1000-8000-00805f9b34fb")
                        )
                    )
                )
            )
        )

        assertEquals(1, plan.size)
        assertEquals(
            "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
            plan.single().characteristicUuid
        )
        assertEquals(
            SafeGattExplorationPlan.Kind.SUBSCRIBE_PROPRIETARY_CHANNEL,
            plan.single().kind
        )
        assertTrue(plan.single().reason.contains("NUS TX"))
        assertFalse(plan.any { it.characteristicUuid.endsWith("6e400002-b5a3-f393-e0a9-e50e24dcca9e") })
        assertFalse(plan.any { it.kind == SafeGattExplorationPlan.Kind.READ_PROPRIETARY_VALUE })
    }

    @Test
    fun deepPlanDoesNotSubscribeNusTxWithoutCccd() {
        val plan = SafeGattExplorationPlan.buildDeep(
            listOf(
                GattServiceRecord(
                    "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
                    listOf(
                        GattCharacteristicRecord(
                            "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
                            setOf("NOTIFY")
                        )
                    )
                )
            )
        )
        assertTrue(plan.isEmpty())
    }

    @Test
    fun doesNotSubscribeWithoutCccd() {
        val plan = SafeGattExplorationPlan.buildDeep(
            listOf(
                GattServiceRecord(
                    "0000180f-0000-1000-8000-00805f9b34fb",
                    listOf(GattCharacteristicRecord(
                        "00002a19-0000-1000-8000-00805f9b34fb",
                        setOf("NOTIFY")
                    ))
                )
            )
        )
        assertTrue(plan.isEmpty())
    }

    @Test
    fun standardPlanDetectsEnvironmentalTemperatureAndBloodPressure() {
        val plan = SafeGattExplorationPlan.buildDeep(
            listOf(
                GattServiceRecord(
                    "0000181a-0000-1000-8000-00805f9b34fb",
                    listOf(GattCharacteristicRecord(
                        "00002a6e-0000-1000-8000-00805f9b34fb",
                        setOf("READ", "NOTIFY"),
                        setOf("00002902-0000-1000-8000-00805f9b34fb")
                    ))
                ),
                GattServiceRecord(
                    "00001810-0000-1000-8000-00805f9b34fb",
                    listOf(GattCharacteristicRecord(
                        "00002a35-0000-1000-8000-00805f9b34fb",
                        setOf("INDICATE"),
                        setOf("00002902-0000-1000-8000-00805f9b34fb")
                    ))
                )
            )
        )
        // Temperature advertises READ and NOTIFY, so both are planned.
        // Blood pressure advertises INDICATE, so it adds one subscription.
        assertEquals(3, plan.size)

        val temperatureOperations = plan.filter {
            it.characteristicUuid.endsWith("2a6e-0000-1000-8000-00805f9b34fb")
        }
        assertEquals(2, temperatureOperations.size)
        assertTrue(temperatureOperations.any {
            it.kind == SafeGattExplorationPlan.Kind.READ_STANDARD_VALUE
        })
        assertTrue(temperatureOperations.any {
            it.kind == SafeGattExplorationPlan.Kind.SUBSCRIBE_STANDARD_MEASUREMENT
        })

        val bloodPressureOperation = plan.single {
            it.characteristicUuid.endsWith("2a35-0000-1000-8000-00805f9b34fb")
        }
        assertEquals(
            SafeGattExplorationPlan.Kind.SUBSCRIBE_STANDARD_MEASUREMENT,
            bloodPressureOperation.kind
        )
        assertTrue(plan.any { it.reason.contains("temperatura ambiente") })
        assertTrue(plan.any { it.reason.contains("pressão arterial") })
    }

    @Test
    fun standardPlanNeverSchedulesDeviceSerialNumber() {
        val plan = SafeGattExplorationPlan.build(
            listOf(
                GattServiceRecord(
                    "0000180a-0000-1000-8000-00805f9b34fb",
                    listOf(GattCharacteristicRecord(
                        "00002a25-0000-1000-8000-00805f9b34fb",
                        setOf("READ")
                    ))
                )
            )
        )
        assertFalse(plan.any { it.characteristicUuid.endsWith("2a25-0000-1000-8000-00805f9b34fb") })
    }

    @Test
    fun deepPlanIncludesBodySensorLocationRead() {
        val plan = SafeGattExplorationPlan.buildDeep(
            listOf(
                GattServiceRecord(
                    "0000180d-0000-1000-8000-00805f9b34fb",
                    listOf(GattCharacteristicRecord(
                        "00002a38-0000-1000-8000-00805f9b34fb",
                        setOf("READ")
                    ))
                )
            )
        )
        assertEquals(1, plan.size)
        assertEquals(SafeGattExplorationPlan.Kind.READ_STANDARD_VALUE, plan.single().kind)
    }


    @Test
    fun deepPlanReadsBodyCompositionFeatureAndSubscribesMeasurement() {
        val plan = SafeGattExplorationPlan.buildDeep(
            listOf(
                GattServiceRecord(
                    "0000181b-0000-1000-8000-00805f9b34fb",
                    listOf(
                        GattCharacteristicRecord(
                            "00002a9b-0000-1000-8000-00805f9b34fb",
                            setOf("READ")
                        ),
                        GattCharacteristicRecord(
                            "00002a9c-0000-1000-8000-00805f9b34fb",
                            setOf("INDICATE"),
                            setOf("00002902-0000-1000-8000-00805f9b34fb")
                        )
                    )
                )
            )
        )
        assertEquals(2, plan.size)
        assertTrue(plan.any {
            it.characteristicUuid.endsWith("2a9b-0000-1000-8000-00805f9b34fb") &&
                it.kind == SafeGattExplorationPlan.Kind.READ_STANDARD_VALUE
        })
        assertTrue(plan.any {
            it.characteristicUuid.endsWith("2a9c-0000-1000-8000-00805f9b34fb") &&
                it.kind == SafeGattExplorationPlan.Kind.SUBSCRIBE_STANDARD_MEASUREMENT
        })
    }


    @Test
    fun deepPlanDoesNotRepeatBaselineBatteryReadButCanSubscribe() {
        val plan = SafeGattExplorationPlan.buildDeep(
            listOf(
                GattServiceRecord(
                    "0000180f-0000-1000-8000-00805f9b34fb",
                    listOf(
                        GattCharacteristicRecord(
                            "00002a19-0000-1000-8000-00805f9b34fb",
                            setOf("READ", "NOTIFY"),
                            setOf("00002902-0000-1000-8000-00805f9b34fb")
                        )
                    )
                )
            )
        )

        assertEquals(1, plan.size)
        assertEquals(
            SafeGattExplorationPlan.Kind.SUBSCRIBE_STANDARD_MEASUREMENT,
            plan.single().kind
        )
    }

    @Test
    fun deepPlanDoesNotSilentlyTruncateCompatibleChannelsAbovePreviousLimit() {
        val services = (0 until 30).map { index ->
            val serviceSuffix = index.toString(16).padStart(4, '0')
            val characteristicSuffix = (0x4a00 + index).toString(16).padStart(4, '0')
            GattServiceRecord(
                "0000" + serviceSuffix + "-0000-1000-8000-00805f9b34fb",
                listOf(GattCharacteristicRecord(
                    "0000" + characteristicSuffix + "-0000-1000-8000-00805f9b34fb",
                    setOf("NOTIFY"),
                    setOf("00002902-0000-1000-8000-00805f9b34fb")
                ))
            )
        }

        val plan = SafeGattExplorationPlan.buildDeep(services)

        assertEquals(30, plan.size)
        assertTrue(plan.all {
            it.kind == SafeGattExplorationPlan.Kind.SUBSCRIBE_PROPRIETARY_CHANNEL
        })
    }

}
