package com.luizmarquesarruda.relogio20.data.discovery

import android.content.Context
import android.content.SharedPreferences
import com.luizmarquesarruda.relogio20.device.discovery.CharacteristicTraffic
import com.luizmarquesarruda.relogio20.device.discovery.DiscoverySnapshot
import com.luizmarquesarruda.relogio20.device.discovery.FeatureDiscoveryEngine
import com.luizmarquesarruda.relogio20.device.discovery.FeatureFinding
import com.luizmarquesarruda.relogio20.device.discovery.GattServiceRecord
import org.json.JSONArray
import org.json.JSONObject

internal sealed class FeatureDiscoveryReadResult {
    object Empty : FeatureDiscoveryReadResult()
    data class Available(val snapshot: DiscoverySnapshot) : FeatureDiscoveryReadResult()
    object Corrupt : FeatureDiscoveryReadResult()
    data class UnsupportedSchema(val version: Int) : FeatureDiscoveryReadResult()
}

/**
 * Stores only GATT inventory and notification metadata.
 * Raw BLE payloads and health measurements are deliberately not persisted.
 * Unknown or future schema data is never overwritten.
 */
internal class FeatureDiscoveryStore(context: Context) {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): FeatureDiscoveryReadResult {
        val raw = preferences.getString(KEY_SNAPSHOT, null)
            ?: return FeatureDiscoveryReadResult.Empty
        return try {
            val root = JSONObject(raw)
            val schema = root.optInt("schemaVersion", -1)
            if (schema > SCHEMA_VERSION) {
                return FeatureDiscoveryReadResult.UnsupportedSchema(schema)
            }
            if (schema != SCHEMA_VERSION) {
                return FeatureDiscoveryReadResult.Corrupt
            }
            FeatureDiscoveryReadResult.Available(decode(root))
        } catch (_: Exception) {
            FeatureDiscoveryReadResult.Corrupt
        }
    }

    fun recordInventory(services: List<GattServiceRecord>, nowMillis: Long): Boolean {
        val previous = when (val state = read()) {
            FeatureDiscoveryReadResult.Empty -> null
            is FeatureDiscoveryReadResult.Available -> state.snapshot
            FeatureDiscoveryReadResult.Corrupt,
            is FeatureDiscoveryReadResult.UnsupportedSchema -> return false
        }
        val snapshot = FeatureDiscoveryEngine.captureInventory(services, nowMillis, previous)
        return persist(snapshot)
    }

    fun recordNotification(
        serviceUuid: String,
        characteristicUuid: String,
        payloadBytes: Int,
        nowMillis: Long,
        frameCommand: Int? = null,
        frameResponse: Int? = null,
        batteryCandidatePercent: Int? = null
    ): Boolean {
        val previous = when (val state = read()) {
            FeatureDiscoveryReadResult.Empty -> FeatureDiscoveryEngine.emptySnapshot(nowMillis)
            is FeatureDiscoveryReadResult.Available -> state.snapshot
            FeatureDiscoveryReadResult.Corrupt,
            is FeatureDiscoveryReadResult.UnsupportedSchema -> return false
        }
        val snapshot = FeatureDiscoveryEngine.observeNotification(
            previous = previous,
            serviceUuid = serviceUuid,
            characteristicUuid = characteristicUuid,
            payloadBytes = payloadBytes,
            observedAtMillis = nowMillis,
            frameCommand = frameCommand,
            frameResponse = frameResponse,
            batteryCandidatePercent = batteryCandidatePercent
        )
        return persist(snapshot)
    }

    private fun persist(snapshot: DiscoverySnapshot): Boolean {
        when (val state = read()) {
            FeatureDiscoveryReadResult.Empty -> Unit
            is FeatureDiscoveryReadResult.Available -> Unit
            FeatureDiscoveryReadResult.Corrupt,
            is FeatureDiscoveryReadResult.UnsupportedSchema -> return false
        }
        return runCatching {
            preferences.edit().putString(KEY_SNAPSHOT, encode(snapshot)).apply()
            true
        }.getOrDefault(false)
    }

    private fun encode(snapshot: DiscoverySnapshot): String {
        val root = JSONObject()
            .put("schemaVersion", snapshot.schemaVersion)
            .put("updatedAtMillis", snapshot.updatedAtMillis)
            .put("serviceCount", snapshot.serviceCount)
            .put("characteristicCount", snapshot.characteristicCount)

        val findings = JSONArray()
        snapshot.findings.forEach { finding ->
            findings.put(
                JSONObject()
                    .put("id", finding.id)
                    .put("category", finding.category)
                    .put("title", finding.title)
                    .put("status", finding.status)
                    .put("detail", finding.detail)
                    .put("evidence", finding.evidence)
                    .put("observationCount", finding.observationCount)
            )
        }
        root.put("findings", findings)

        val traffic = JSONArray()
        snapshot.traffic.forEach { item ->
            val json = JSONObject()
                .put("serviceUuid", item.serviceUuid)
                .put("characteristicUuid", item.characteristicUuid)
                .put("notificationCount", item.notificationCount)
                .put("lastSeenAtMillis", item.lastSeenAtMillis)
                .put("lastPayloadBytes", item.lastPayloadBytes)
            putNullableInt(json, "lastFrameCommand", item.lastFrameCommand)
            putNullableInt(json, "lastFrameResponse", item.lastFrameResponse)
            putNullableInt(json, "batteryCandidatePercent", item.batteryCandidatePercent)
            traffic.put(json)
        }
        root.put("traffic", traffic)
        return root.toString()
    }

    private fun decode(root: JSONObject): DiscoverySnapshot {
        val findingsJson = root.optJSONArray("findings") ?: JSONArray()
        val findings = buildList {
            for (index in 0 until findingsJson.length()) {
                val item = findingsJson.getJSONObject(index)
                add(
                    FeatureFinding(
                        id = item.getString("id"),
                        category = item.optString("category"),
                        title = item.optString("title"),
                        status = item.optString("status"),
                        detail = item.optString("detail"),
                        evidence = item.optString("evidence"),
                        observationCount = item.optInt("observationCount", 0).coerceAtLeast(0)
                    )
                )
            }
        }

        val trafficJson = root.optJSONArray("traffic") ?: JSONArray()
        val traffic = buildList {
            for (index in 0 until trafficJson.length()) {
                val item = trafficJson.getJSONObject(index)
                add(
                    CharacteristicTraffic(
                        serviceUuid = item.getString("serviceUuid"),
                        characteristicUuid = item.getString("characteristicUuid"),
                        notificationCount = item.optInt("notificationCount", 0).coerceAtLeast(0),
                        lastSeenAtMillis = item.optLong("lastSeenAtMillis", 0L).coerceAtLeast(0L),
                        lastPayloadBytes = item.optInt("lastPayloadBytes", 0).coerceAtLeast(0),
                        lastFrameCommand = nullableInt(item, "lastFrameCommand"),
                        lastFrameResponse = nullableInt(item, "lastFrameResponse"),
                        batteryCandidatePercent = nullableInt(item, "batteryCandidatePercent")
                    )
                )
            }
        }

        return DiscoverySnapshot(
            schemaVersion = SCHEMA_VERSION,
            updatedAtMillis = root.optLong("updatedAtMillis", 0L).coerceAtLeast(0L),
            serviceCount = root.optInt("serviceCount", 0).coerceAtLeast(0),
            characteristicCount = root.optInt("characteristicCount", 0).coerceAtLeast(0),
            findings = findings.take(MAX_FINDINGS),
            traffic = traffic.take(MAX_TRAFFIC)
        )
    }

    private fun putNullableInt(json: JSONObject, key: String, value: Int?) {
        if (value == null) json.put(key, JSONObject.NULL) else json.put(key, value)
    }

    private fun nullableInt(json: JSONObject, key: String): Int? =
        if (!json.has(key) || json.isNull(key)) null else json.optInt(key)

    companion object {
        private const val PREFERENCES_NAME = "feature_discovery_v1"
        private const val KEY_SNAPSHOT = "gatt_discovery_snapshot"
        private const val SCHEMA_VERSION = 1
        private const val MAX_FINDINGS = 160
        private const val MAX_TRAFFIC = 128
    }
}
