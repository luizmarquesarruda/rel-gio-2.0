package com.luizmarquesarruda.relogio20.device.ble.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GattCallbackSafetyPolicyTest {
    @Test
    fun acceptsOnlyCallbackForCurrentConnectionAndExactActiveOperation() {
        assertTrue(
            GattCallbackSafetyPolicy.acceptsOperationCallback(
                connectionIsCurrent = true,
                activeOperationKey = "180f/2a19/READ",
                callbackOperationKey = "180f/2a19/READ"
            )
        )
        assertFalse(
            GattCallbackSafetyPolicy.acceptsOperationCallback(
                connectionIsCurrent = false,
                activeOperationKey = "180f/2a19/READ",
                callbackOperationKey = "180f/2a19/READ"
            )
        )
        assertFalse(
            GattCallbackSafetyPolicy.acceptsOperationCallback(
                connectionIsCurrent = true,
                activeOperationKey = null,
                callbackOperationKey = "180f/2a19/READ"
            )
        )
        assertFalse(
            GattCallbackSafetyPolicy.acceptsOperationCallback(
                connectionIsCurrent = true,
                activeOperationKey = "180f/2a19/READ",
                callbackOperationKey = "180f/2a19/CCCD"
            )
        )
        assertFalse(
            GattCallbackSafetyPolicy.acceptsOperationCallback(
                connectionIsCurrent = true,
                activeOperationKey = "180f/2a19/READ",
                callbackOperationKey = " "
            )
        )
    }

    @Test
    fun rejectsLateCallbackForOperationInvalidatedWithinSameGattConnection() {
        val key = "180f/2a19/READ"
        assertFalse(
            GattCallbackSafetyPolicy.acceptsOperationCallback(
                connectionIsCurrent = true,
                activeOperationKey = key,
                callbackOperationKey = key,
                invalidatedOperationKeys = setOf(key)
            )
        )
        assertTrue(
            GattCallbackSafetyPolicy.acceptsOperationCallback(
                connectionIsCurrent = true,
                activeOperationKey = key,
                callbackOperationKey = key,
                invalidatedOperationKeys = emptySet()
            )
        )
    }

    @Test
    fun descriptorWriteCallbackKeepsEnableAndDisableOperationsDistinct() {
        val enable = "180d/2a37/CCCD_ENABLE"
        val disable = "180d/2a37/CCCD_DISABLE"

        assertEquals(
            enable,
            GattCallbackSafetyPolicy.descriptorWriteOperationKey(enable, enable, disable)
        )
        assertEquals(
            disable,
            GattCallbackSafetyPolicy.descriptorWriteOperationKey(disable, enable, disable)
        )
        assertNull(
            GattCallbackSafetyPolicy.descriptorWriteOperationKey(null, enable, disable)
        )
        assertNull(
            GattCallbackSafetyPolicy.descriptorWriteOperationKey("other", enable, disable)
        )
    }

    @Test
    fun rejectsNotificationsFromStaleDisconnectedOrDisconnectingSession() {
        assertTrue(
            GattCallbackSafetyPolicy.acceptsNotificationCallback(
                connectionIsCurrent = true,
                connected = true,
                disconnecting = false
            )
        )
        assertFalse(
            GattCallbackSafetyPolicy.acceptsNotificationCallback(
                connectionIsCurrent = false,
                connected = true,
                disconnecting = false
            )
        )
        assertFalse(
            GattCallbackSafetyPolicy.acceptsNotificationCallback(
                connectionIsCurrent = true,
                connected = false,
                disconnecting = false
            )
        )
        assertFalse(
            GattCallbackSafetyPolicy.acceptsNotificationCallback(
                connectionIsCurrent = true,
                connected = true,
                disconnecting = true
            )
        )
    }

    @Test
    fun rediscoversOnlyForCurrentConnectedNonHandoffSession() {
        assertTrue(
            GattCallbackSafetyPolicy.shouldRediscoverServices(
                connectionIsCurrent = true,
                connected = true,
                disconnecting = false,
                handoffActive = false
            )
        )
        assertFalse(
            GattCallbackSafetyPolicy.shouldRediscoverServices(
                connectionIsCurrent = false,
                connected = true,
                disconnecting = false,
                handoffActive = false
            )
        )
        assertFalse(
            GattCallbackSafetyPolicy.shouldRediscoverServices(
                connectionIsCurrent = true,
                connected = false,
                disconnecting = false,
                handoffActive = false
            )
        )
        assertFalse(
            GattCallbackSafetyPolicy.shouldRediscoverServices(
                connectionIsCurrent = true,
                connected = true,
                disconnecting = true,
                handoffActive = false
            )
        )
        assertFalse(
            GattCallbackSafetyPolicy.shouldRediscoverServices(
                connectionIsCurrent = true,
                connected = true,
                disconnecting = false,
                handoffActive = true
            )
        )
    }
}
