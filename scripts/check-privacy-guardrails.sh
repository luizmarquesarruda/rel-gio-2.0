#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

REPORT_DIR="app/build/reports/security"
mkdir -p "$REPORT_DIR"

BLOCKED_DEPENDENCY_PATTERN='com\.(bytedance|volcengine|sigmob|umeng|pangle|amap|baidu|alipay|wechat|qq\.e|tencent\.ads|adjust|appsflyer|mixpanel|amplitude|onesignal|facebook|braze|segment|customerio|google\.firebase\.(analytics|crashlytics))|cn\.jpush|io\.sentry|io\.branch'

for configuration in \
  diagnosticDebugRuntimeClasspath \
  companionDebugRuntimeClasspath \
  diagnosticReleaseRuntimeClasspath \
  companionReleaseRuntimeClasspath \
  diagnosticBenchmarkRuntimeClasspath; do
  report="$REPORT_DIR/${configuration}.txt"
  echo "Auditing Gradle configuration: $configuration"
  gradle --no-daemon --console=plain :app:dependencies --configuration "$configuration" > "$report"

  if grep -Eiq "$BLOCKED_DEPENDENCY_PATTERN" "$report"; then
    echo "Blocked advertising/marketing/analytics SDK namespace found in $configuration." >&2
    grep -Ein "$BLOCKED_DEPENDENCY_PATTERN" "$report" >&2 || true
    exit 1
  fi
done

# LeakCanary is permitted only in diagnosticDebug. It can retain heap dumps containing
# local health, BLE, or notification data, so release and all other variants must exclude it.
if ! grep -Fq 'com.squareup.leakcanary:leakcanary-android:2.14' \
  "$REPORT_DIR/diagnosticDebugRuntimeClasspath.txt"; then
  echo "LeakCanary 2.14 is missing from diagnosticDebugRuntimeClasspath." >&2
  exit 1
fi

for configuration in \
  companionDebugRuntimeClasspath \
  diagnosticReleaseRuntimeClasspath \
  companionReleaseRuntimeClasspath \
  diagnosticBenchmarkRuntimeClasspath; do
  report="$REPORT_DIR/${configuration}.txt"
  if grep -Eiq 'com\.squareup\.leakcanary|leakcanary-android' "$report"; then
    echo "LeakCanary must not be present in $configuration." >&2
    grep -Ein 'leakcanary' "$report" >&2 || true
    exit 1
  fi
done

SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$SDK_ROOT" ]]; then
  echo "Android SDK path unavailable; cannot inspect APK permissions." >&2
  exit 1
fi

AAPT="$(find "$SDK_ROOT/build-tools" -type f -name aapt | sort -V | tail -n 1)"
if [[ -z "$AAPT" ]]; then
  echo "aapt not found in Android SDK build-tools." >&2
  exit 1
fi

for variant in diagnostic companion; do
  apk="app/build/outputs/apk/$variant/debug/app-$variant-debug.apk"
  if [[ ! -f "$apk" ]]; then
    echo "Expected APK not found: $apk" >&2
    exit 1
  fi

  permissions="$REPORT_DIR/$variant-permissions.txt"
  "$AAPT" dump permissions "$apk" > "$permissions"

  if grep -Eiq 'android\.permission\.(READ_PHONE_STATE|READ_PHONE_NUMBERS|CALL_PHONE|READ_CALL_LOG|WRITE_CALL_LOG|READ_SMS|RECEIVE_SMS|SEND_SMS|RECEIVE_MMS|RECEIVE_WAP_PUSH|READ_CONTACTS|WRITE_CONTACTS|GET_ACCOUNTS|ACCESS_BACKGROUND_LOCATION|QUERY_ALL_PACKAGES|MANAGE_EXTERNAL_STORAGE|REQUEST_INSTALL_PACKAGES|SYSTEM_ALERT_WINDOW|RECORD_AUDIO|CAMERA|READ_MEDIA_IMAGES|READ_MEDIA_VIDEO|READ_MEDIA_AUDIO|READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE)|com\.google\.android\.gms\.permission\.AD_ID' "$permissions"; then
    echo "Unapproved sensitive permission found in $apk." >&2
    grep -Ein 'android\.permission\.(READ_PHONE_STATE|READ_PHONE_NUMBERS|CALL_PHONE|READ_CALL_LOG|WRITE_CALL_LOG|READ_SMS|RECEIVE_SMS|SEND_SMS|RECEIVE_MMS|RECEIVE_WAP_PUSH|READ_CONTACTS|WRITE_CONTACTS|GET_ACCOUNTS|ACCESS_BACKGROUND_LOCATION|QUERY_ALL_PACKAGES|MANAGE_EXTERNAL_STORAGE|REQUEST_INSTALL_PACKAGES|SYSTEM_ALERT_WINDOW|RECORD_AUDIO|CAMERA|READ_MEDIA_IMAGES|READ_MEDIA_VIDEO|READ_MEDIA_AUDIO|READ_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE)|com\.google\.android\.gms\.permission\.AD_ID' "$permissions" >&2 || true
    exit 1
  fi
done

echo "Privacy dependency and APK permission guardrails passed."
