#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

for configuration in diagnosticDebugRuntimeClasspath companionDebugRuntimeClasspath; do
  report="$(mktemp)"
  trap 'rm -f "$report"' EXIT
  echo "Auditing Gradle configuration: $configuration"
  gradle --no-daemon --console=plain :app:dependencies --configuration "$configuration" > "$report"

  if grep -Eiq 'com\.(bytedance|volcengine|sigmob|umeng|pangle|amap|baidu|alipay|wechat|qq\.e)|cn\.jpush|com\.tencent\.ads' "$report"; then
    echo "Blocked advertising/marketing/analytics SDK namespace found in $configuration." >&2
    grep -Ein 'com\.(bytedance|volcengine|sigmob|umeng|pangle|amap|baidu|alipay|wechat|qq\.e)|cn\.jpush|com\.tencent\.ads' "$report" >&2 || true
    exit 1
  fi
  rm -f "$report"
  trap - EXIT
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

for apk in \
  app/build/outputs/apk/diagnostic/debug/app-diagnostic-debug.apk \
  app/build/outputs/apk/companion/debug/app-companion-debug.apk; do
  if [[ ! -f "$apk" ]]; then
    echo "Expected APK not found: $apk" >&2
    exit 1
  fi
  permissions="$(mktemp)"
  "$AAPT" dump permissions "$apk" > "$permissions"
  if grep -Eiq 'android\.permission\.(READ_PHONE_STATE|READ_CALL_LOG|READ_SMS|RECEIVE_SMS|READ_CONTACTS|ACCESS_BACKGROUND_LOCATION|QUERY_ALL_PACKAGES)|com\.google\.android\.gms\.permission\.AD_ID' "$permissions"; then
    echo "Unapproved sensitive permission found in $apk." >&2
    grep -Ein 'android\.permission\.(READ_PHONE_STATE|READ_CALL_LOG|READ_SMS|RECEIVE_SMS|READ_CONTACTS|ACCESS_BACKGROUND_LOCATION|QUERY_ALL_PACKAGES)|com\.google\.android\.gms\.permission\.AD_ID' "$permissions" >&2 || true
    rm -f "$permissions"
    exit 1
  fi
  rm -f "$permissions"
done

echo "Privacy dependency and APK permission guardrails passed."
