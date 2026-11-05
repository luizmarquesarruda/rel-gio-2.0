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
 * Stores only bounded GATT inventory and notification metadata.
 * Raw BLE payloads and health measurements are deliberately not persisted.
 * Invalid, corrupt, or future-schema data is never replaced.
 */
internal class FeatureDiscoveryStore(context: Context) {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): FeatureDiscoveryReadResult = synchronized(STORE_LOCK) {
        val raw = try {
            preferences.getString(KEY_SNAPSHOT, null)
        } catch (_: Exception) {
            return@synchronized FeatureDiscoveryReadResult.Corrupt
        } ?: return@synchronized FeatureDiscoveryReadResult.Empty

        if (raw.length > MAX_JSON_CHARS) return@synchronized FeatureDiscoveryReadResult.Corrupt
        try {
            val root = JSONObject(raw)
            val schema = requiredInt(root, "schemaVersion")
            if (schema > FeatureDiscoverySnapshotPolicy.SCHEMA_VERSION) {
                return@synchronized FeatureDiscoveryReadResult.UnsupportedSchema(schema)
            }
            if (schema != FeatureDiscoverySnapshotPolicy.SCHEMA_VERSION) {
                return@synchronized FeatureDiscoveryReadResult.Corrupt
            }
            val snapshot = decode(root)
            if (!FeatureDiscoverySnapshotPolicy.isValid(snapshot)) {
                return@synchronized FeatureDiscoveryReadResult.Corrupt
            }
            FeatureDiscoveryReadResult.Available(snapshot)
        } catch (_: Exception) {
            FeatureDiscoveryReadResult.Corrupt
        }
    }

    fun recordInventory(services: List<GattServiceRecord>, nowMillis: Long): Boolean =
        synchronized(STORE_LOCK) {
            if (nowMillis < 0L ||
                services.size > MAX_SERVICES ||
                services.sumOf { it.characteristics.size.toLong() } > MAX_CHARACTERISTICS.toLong()
            ) return@synchronized false

            val previous = when (val state = read()) {
                FeatureDiscoveryReadResult.Empty -> null
                is FeatureDiscoveryReadResult.Available -> state.snapshot
                FeatureDiscoveryReadResult.Corrupt,
                is FeatureDiscoveryReadResult.UnsupportedSchema -> return@synchronized false
            }
            val snapshot = FeatureDiscoveryEngine.captureInventory(services, nowMillis, previous)
            persist(snapshot)
        }

    fun recordNotification(
        serviceUuid: String,
        characteristicUuid: String,
        payloadBytes: Int,
        nowMillis: Long,
        frameCommand: Int? = null,
        frameResponse: Int? = null,
        batteryCandidatePercent: Int? = null
    ): Boolean = synchronized(STORE_LOCK) {
        if (nowMillis < 0L ||
            !isValidUuidToken(serviceUuid) ||
            !isValidUuidToken(characteristicUuid) ||
            payloadBytes !in 0..MAX_PAYLOAD_BYTES ||
            frameCommand?.let { it !in 0..255 } == true ||
            frameResponse?.let { it !in 0..255 } == true ||
            (frameCommand == null) != (frameResponse == null) ||
            batteryCandidatePercent?.let { it !in 0..100 } == true
        ) return@synchronized false

        val previous = when (val state = read()) {
            FeatureDiscoveryReadResult.Empty -> FeatureDiscoveryEngine.emptySnapshot(nowMillis)
            is FeatureDiscoveryReadResult.Available -> state.snapshot
            is FeatureDiscoveryReadResult.Corrupt,
            is FeatureDiscoveryReadResult.UnsupportedSchema -> return@synchronized false
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
        persist(snapshot)
    }

    private fun persist(snapshot: DiscoverySnapshot): Boolean {
        if (!FeatureDiscoverySnapshotPolicy.isValid(snapshot)) return false
        when (read()) {
            FeatureDiscoveryReadResult.Empty,
            is FeatureDiscoveryReadResult.Available -> Unit
            FeatureDiscoveryReadResult.Corrupt,
            is FeatureDiscoveryReadResult.UnsupportedSchema -> return false
        }
        return runCatching {
            preferences.edit().putString(KEY_SNAPSHOT, encode(snapshot)).commit()
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
        val findingsJson = root.get("findings") as? JSONArray
            ?: error("Campo findings ausente ou inválido.")
        val trafficJson = root.get("traffic") as? JSONArray
            ?: error("Campo traffic ausente ou inválido.")
        if (findingsJson.length() > FeatureDiscoverySnapshotPolicy.MAX_FINDINGS ||
            trafficJson.length() > FeatureDiscoverySnapshotPolicy.MAX_TRAFFIC
        ) error("Inventário excede os limites do schema.")

        val findings = buildList {
            for (index in 0 until findingsJson.length()) {
                val item = findingsJson.getJSONObject(index)
                add(
                    FeatureFinding(
                        id = requiredString(item, "id"),
                        category = requiredString(item, "category"),
                        title = requiredString(item, "title"),
                        status = requiredString(item, "status"),
                        detail = requiredString(item, "detail"),
                        evidence = requiredString(item, "evidence"),
                        observationCount = requiredInt(item, "observationCount")
                    )
                )
            }
        }

        val traffic = buildList {
            for (index in 0 until trafficJson.length()) {
                val item = trafficJson.getJSONObject(index)
                add(
                    CharacteristicTraffic(
                        serviceUuid = requiredString(item, "serviceUuid"),
                        characteristicUuid = requiredString(item, "characteristicUuid"),
                        notificationCount = requiredInt(item, "notificationCount"),
                        lastSeenAtMillis = requiredLong(item, "lastSeenAtMillis"),
                        lastPayloadBytes = requiredInt(item, "lastPayloadBytes"),
                        lastFrameCommand = nullableInt(item, "lastFrameCommand"),
                        lastFrameResponse = nullableInt(item, "lastFrameResponse"),
                        batteryCandidatePercent = nullableInt(item, "batteryCandidatePercent")
                    )
                )
            }
        }

        return DiscoverySnapshot(
            schemaVersion = requiredInt(root, "schemaVersion"),
            updatedAtMillis = requiredLong(root, "updatedAtMillis"),
            serviceCount = requiredInt(root, "serviceCount"),
            characteristicCount = requiredInt(root, "characteristicCount"),
            findings = findings,
            traffic = traffic
        )
    }

    private fun requiredString(json: JSONObject, key: String): String =
        json.get(key) as? String ?: error("Campo $key ausente ou inválido.")

    private fun requiredInt(json: JSONObject, key: String): Int {
        val value = requiredLong(json, key)
        if (value !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
            error("Campo $key excede Int.")
        }
        return value.toInt()
    }

    private fun requiredLong(json: JSONObject, key: String): Long {
        val value = json.get(key) as? Number ?: error("Campo $key não é numérico.")
        val numeric = value.toDouble()
        if (!numeric.isFinite() || numeric % 1.0 != 0.0) error("Campo $key não é inteiro.")
        if (value is Double || value is Float) {
            if (numeric < Long.MIN_VALUE.toDouble() || numeric > MAX_TIMESTAMP.toDouble()) {
                error("Campo $key está fora do limite.")
            }
        }
        return value.toLong().also {
            if (it < 0L || it > MAX_TIMESTAMP) error("Campo $key está fora do limite.")
        }
    }

    private fun putNullableInt(json: JSONObject, key: String, value: Int?) {
        if (value == null) json.put(key, JSONObject.NULL) else json.put(key, value)
    }

    private fun nullableInt(json: JSONObject, key: String): Int? {
        if (!json.has(key)) error("Campo $key ausente.")
        if (json.isNull(key)) return null
        return requiredInt(json, key)
    }

    private fun isValidUuidToken(value: String): Boolean =
        value.isNotBlank() && value.length <= 64 &&
            value.none { it.isWhitespace() || Character.isISOControl(it) }

    companion object {
        private const val PREFERENCES_NAME = "feature_discovery_v1"
        private const val KEY_SNAPSHOT = "gatt_discovery_snapshot"
        private const val MAX_JSON_CHARS = 1_000_000
        private const val MAX_SERVICES = 1_024
        private const val MAX_CHARACTERISTICS = 8_192
        private const val MAX_PAYLOAD_BYTES = 65_535
        private const val MAX_TIMESTAMP = FeatureDiscoverySnapshotPolicy.MAX_TIMESTAMP
        private val STORE_LOCK = Any()
    }
}
