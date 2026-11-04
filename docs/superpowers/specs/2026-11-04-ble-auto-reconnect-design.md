# BLE Automatic Reconnection Design

**Date:** 2026-11-04

## Intent
After the GT3 MAX Call has completed its first successful GATT connection, Relógio 2.0 should restore that connection automatically after an unexpected loss. The user should not need to repeat the manual scan.

## Goals
- Persist locally that this exact configured device has connected successfully before.
- Retry only the normalized Bluetooth address already stored in the device identity preferences.
- Use retry delays of 1, 2, 5, 10, 20, then 30 seconds, capped at 30 seconds for subsequent attempts.
- Reset the delay sequence after a successful GATT connection.
- Restart the recovery flow when the app returns to the foreground or is reopened.
- Stop automatic reconnection immediately when the user requests Desconectar.
- Preserve the current passive GATT inventory and the single approved NUS TX notification subscription.

## Non-goals
- No writes to NUS RX or unknown/private characteristics.
- No firmware, DFU/OTA, reset, clock-setting, sensor polling, or proprietary commands.
- No additional Android permissions, SDKs, or background service in this phase.
- No claim that BLE/GATT connectivity validates sensor readings.

## Approaches considered
1. **Exact-address foreground scan with bounded backoff (selected).** Reuses the existing scan and identity gate, gives deterministic address filtering, and avoids new permissions or background-service behavior.
2. Android connectGatt(autoConnect=true). This delegates retry timing to the platform and is less observable/controlable in the existing diagnostic UI; it does not replace the need to restore GATT subscriptions.
3. Companion Device Manager plus a connected-device foreground service. This is a better future direction for durable background presence, but adds association UX, service lifecycle, notification and Android-version policy work beyond this narrowly scoped change.

## Behavior and constraints
- A successful GATT STATE_CONNECTED enables and persists automatic reconnection.
- Unexpected GATT disconnects, connection failures after a prior successful connection, and automatic scans that do not find the configured address schedule another attempt.
- The app does not prompt for permissions in an automatic retry. If Bluetooth permissions are missing, it pauses until the user restores permission and returns to the app.
- Automatic scanning runs only while the app is in the foreground. A live GATT connection is not deliberately closed on pause; a disconnect while paused is recovered on resume. Reopening the app also resumes the stored reconnect preference.
- A user-initiated disconnect clears the persisted reconnect preference and cancels pending retries. The next successful user-initiated connection enables it again.
- No device other than the exact locally configured address can be connected by the recovery loop.
- This phase does not guarantee reconnection while the app is force-stopped or continuously in the background. That requires a separately reviewed Android companion/foreground-service design.

## Validation
Unit tests cover the retry sequence, 30-second cap, reset after connection restoration, disabled initial state, persisted enabled state, and manual-disconnect suppression. GitHub Actions must pass Kotlin unit tests, lint, both app variants, privacy guardrails, APK manifest/signature checks, and artifact packaging. Physical reconnect testing on the Galaxy A06 / Android 16 with the GT3 MAX Call remains a separate required validation and must be reported as not validated until performed.