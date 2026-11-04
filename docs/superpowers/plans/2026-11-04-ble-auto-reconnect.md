# BLE Automatic Reconnection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Automatically reconnect to the user's previously connected GT3 MAX Call after unexpected BLE/GATT loss.

**Architecture:** Add a pure Kotlin retry policy in the BLE core, then integrate it into the existing Activity-owned scan/GATT lifecycle. Store the enabled state only in local app preferences and reuse the exact-address identity gate.

**Tech Stack:** Kotlin, Android BLE/GATT, Android Handler, JUnit 4, GitHub Actions.

**Spec:** docs/superpowers/specs/2026-11-04-ble-auto-reconnect-design.md

## Global Constraints
- Do not add Android permissions, services, dependencies, or network access.
- Do not write a payload to NUS RX or any unknown/private GATT characteristic.
- Retry only the locally configured GT3 MAX Bluetooth address.
- Use delays 1, 2, 5, 10, 20, and 30 seconds, capped at 30 seconds.
- Manual disconnect disables and persists the disabled state.
- Preserve existing NUS TX Notify + standard CCCD behavior and all sensor-validation labels.

## Review Focus
- A GATT error callback (including status 147) must clear the stale GATT and schedule retry, not leave the app stuck.
- A missing Bluetooth permission must not cause an automatic permission prompt loop.
- Bluetooth disabled or scanner unavailable must not crash or create overlapping scans.
- A scan callback arriving after pause/cancel must not start a stale GATT connection.
- Manual disconnect must win over a delayed callback or pending retry.

---

### Task 1: Retry policy and tests

**Files:**
- Create: app/src/main/java/com/luizmarquesarruda/relogio20/device/ble/core/BleReconnectPolicy.kt
- Test: app/src/test/java/com/luizmarquesarruda/relogio20/device/ble/core/BleReconnectPolicyTest.kt

**Interfaces:**
- Produces: BleReconnectPolicy(initiallyEnabled: Boolean = false)
- Produces: markConnected(), markManuallyDisconnected(), nextRetryDelayMillis(): Long?

- [ ] Write tests for disabled initial state, retry progression/cap, reset after connection, persisted enabled state, and manual disconnect.
- [ ] Run the focused JUnit test and confirm it fails because the policy is not implemented.
- [ ] Implement the minimal pure Kotlin policy.
- [ ] Run the focused JUnit test and the full Kotlin unit-test tasks.

### Task 2: Activity BLE lifecycle integration

**Files:**
- Modify: app/src/main/java/com/luizmarquesarruda/relogio20/MainActivity.kt

**Interfaces:**
- Consumes: BleReconnectPolicy from Task 1.
- Uses persisted preference key auto_reconnect_enabled in device_connection.

- [ ] Persist reconnect enablement after successful GATT connection and restore it on Activity creation.
- [ ] Schedule retries after unexpected GATT failure/disconnect and after a reconnect scan finds no matching address.
- [ ] Use shorter, low-power scans for recovery; preserve the existing 15-second user-started scan.
- [ ] Resume recovery on foreground return; defer automatic scans while paused.
- [ ] Cancel all delayed work and disable persistence on user-requested disconnect.
- [ ] Add a connection-attempt timeout and ensure stale callbacks cannot revive a cancelled attempt.
- [ ] Run Kotlin tests and lint.

### Task 3: Documentation and regression review

**Files:**
- Modify: README.md
- Modify: CHANGELOG.md
- Modify: docs/DIARIO-DE-BORDO.md
- Modify: docs/MAPA-DE-MODULOS.md

- [ ] Document the retry behavior and foreground-only limit without claiming background/force-stop guarantees.
- [ ] Verify the read-only GATT policy and no-added-permission constraints are still true.
- [ ] Run all CI checks and inspect the final diff and workflow status.
- [ ] Record physical Galaxy A06 / GT3 MAX Call testing as não validado until performed.