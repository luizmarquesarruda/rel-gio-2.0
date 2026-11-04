#!/usr/bin/env python3
"""Audit a decoded Android APK manifest against the Relogio 2.0 policy."""

from __future__ import annotations

import argparse
import re
from dataclasses import dataclass
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ANDROID_NS = "http://schemas.android.com/apk/res/android"
ANDROID = f"{{{ANDROID_NS}}}"
NOTIFICATION_LISTENER_ACTION = "android.service.notification.NotificationListenerService"
NOTIFICATION_LISTENER_PERMISSION = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
ACCESSIBILITY_ACTION = "android.accessibilityservice.AccessibilityService"
ACCESSIBILITY_PERMISSION = "android.permission.BIND_ACCESSIBILITY_SERVICE"

# These capabilities are outside the approved, local-first scope of this app.
# Any future exception must be reviewed and documented before changing this set.
SENSITIVE_PERMISSIONS = {
    "android.permission.READ_SMS",
    "android.permission.RECEIVE_SMS",
    "android.permission.SEND_SMS",
    "android.permission.RECEIVE_MMS",
    "android.permission.RECEIVE_WAP_PUSH",
    "android.permission.READ_PHONE_STATE",
    "android.permission.READ_PHONE_NUMBERS",
    "android.permission.CALL_PHONE",
    "android.permission.READ_CALL_LOG",
    "android.permission.WRITE_CALL_LOG",
    "android.permission.READ_CONTACTS",
    "android.permission.WRITE_CONTACTS",
    "android.permission.GET_ACCOUNTS",
    "android.permission.ACCESS_BACKGROUND_LOCATION",
    "android.permission.QUERY_ALL_PACKAGES",
    "com.google.android.gms.permission.AD_ID",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.REQUEST_INSTALL_PACKAGES",
    "android.permission.SYSTEM_ALERT_WINDOW",
    "android.permission.RECORD_AUDIO",
    "android.permission.CAMERA",
    "android.permission.READ_MEDIA_IMAGES",
    "android.permission.READ_MEDIA_VIDEO",
    "android.permission.READ_MEDIA_AUDIO",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
}


@dataclass(frozen=True)
class AuditIssue:
    code: str
    message: str


def _attr(element: ET.Element, name: str) -> str | None:
    return element.attrib.get(ANDROID + name)


def _bool_attr(element: ET.Element, name: str) -> bool | None:
    value = _attr(element, name)
    if value is None:
        return None
    return value.strip().lower() == "true"


def _service_actions(service: ET.Element) -> set[str]:
    return {
        action_name
        for action in service.findall(".//action")
        if (action_name := _attr(action, "name"))
    }


def audit_manifest(manifest_xml: str, variant: str) -> list[AuditIssue]:
    """Return policy violations for a decoded APK manifest XML document."""
    if variant not in {"diagnostic", "companion"}:
        raise ValueError("variant must be 'diagnostic' or 'companion'")

    # apkanalyzer manifest print may emit raw ampersands from decoded Android
    # string resources (for example, "Saúde & Fitness") instead of XML entities.
    # Escape only ampersands that are not already valid XML entities so the
    # policy audit can inspect the APK manifest without changing its semantics.
    xml_safe_manifest = re.sub(
        r"&(?!amp;|lt;|gt;|quot;|apos;|#\d+;|#x[0-9A-Fa-f]+;)",
        "&amp;",
        manifest_xml,
    )
    try:
        root = ET.fromstring(xml_safe_manifest)
    except ET.ParseError as error:
        return [AuditIssue("manifest_parse_error", f"Invalid APK manifest XML: {error}")]

    if root.tag != "manifest":
        return [AuditIssue("manifest_root_invalid", "APK manifest root element is not <manifest>.")]

    issues: list[AuditIssue] = []
    expected_package = (
        "com.luizmarquesarruda.relogio20.diagnostic"
        if variant == "diagnostic"
        else "com.luizmarquesarruda.relogio20"
    )
    actual_package = root.attrib.get("package")
    if actual_package != expected_package:
        issues.append(AuditIssue(
            "variant_package_id_mismatch",
            f"Expected package {expected_package}, found {actual_package or '(missing)'}.",
        ))

    for permission_tag in (
        "uses-permission",
        "uses-permission-sdk-23",
        "uses-permission-sdk-m",
    ):
        for permission in root.findall(permission_tag):
            name = _attr(permission, "name") or ""
            if name == "android.permission.INTERNET":
                issues.append(AuditIssue(
                    "network_permission_not_approved",
                    "INTERNET is not approved: this build is local-first and has no network feature.",
                ))
            elif name in SENSITIVE_PERMISSIONS:
                issues.append(AuditIssue(
                    "sensitive_permission_not_approved",
                    f"Sensitive permission is outside the approved scope: {name}",
                ))
            elif name == "android.permission.ACCESS_FINE_LOCATION":
                max_sdk = _attr(permission, "maxSdkVersion")
                try:
                    limited_to_legacy = max_sdk is not None and int(max_sdk) <= 30
                except ValueError:
                    limited_to_legacy = False
                if not limited_to_legacy:
                    issues.append(AuditIssue(
                        "legacy_location_permission_not_limited",
                        "ACCESS_FINE_LOCATION must be capped at maxSdkVersion=30 for legacy BLE only.",
                    ))

    application = root.find("application")
    if application is None:
        issues.append(AuditIssue("application_missing", "APK manifest has no <application> element."))
        return issues

    if _bool_attr(application, "allowBackup") is not False:
        issues.append(AuditIssue(
            "backup_must_be_disabled",
            "android:allowBackup must be explicitly false for this app.",
        ))

    if _bool_attr(application, "usesCleartextTraffic") is True:
        issues.append(AuditIssue(
            "cleartext_traffic_enabled",
            "Cleartext network traffic must not be enabled.",
        ))

    notification_services: list[ET.Element] = []
    for service in application.findall("service"):
        actions = _service_actions(service)
        service_permission = _attr(service, "permission")

        is_notification_listener = (
            NOTIFICATION_LISTENER_ACTION in actions
            or service_permission == NOTIFICATION_LISTENER_PERMISSION
        )
        if is_notification_listener:
            notification_services.append(service)

        if service_permission == ACCESSIBILITY_PERMISSION or ACCESSIBILITY_ACTION in actions:
            issues.append(AuditIssue(
                "accessibility_service_not_approved",
                "Accessibility Service is not approved for this project's current scope.",
            ))

    if variant == "diagnostic" and notification_services:
        issues.append(AuditIssue(
            "diagnostic_notification_listener_present",
            "Diagnostic APK must not declare NotificationListenerService or its binding permission.",
        ))

    if variant == "companion":
        action_services = [
            service for service in notification_services
            if NOTIFICATION_LISTENER_ACTION in _service_actions(service)
        ]
        if len(action_services) != 1:
            issues.append(AuditIssue(
                "companion_notification_listener_missing",
                "Companion APK must declare exactly one NotificationListenerService implementation.",
            ))
        for service in action_services:
            service_name = _attr(service, "name") or ""
            if not service_name.endswith(".notifications.NotificationCaptureService"):
                issues.append(AuditIssue(
                    "unexpected_notification_listener_class",
                    f"Unexpected NotificationListenerService implementation: {service_name or '(unnamed)'}",
                ))
            if _attr(service, "permission") != NOTIFICATION_LISTENER_PERMISSION:
                issues.append(AuditIssue(
                    "notification_listener_permission_missing",
                    "NotificationListenerService must be protected by BIND_NOTIFICATION_LISTENER_SERVICE.",
                ))
            if _attr(service, "exported") != "false":
                issues.append(AuditIssue(
                    "notification_listener_must_not_be_exported",
                    "NotificationListenerService must explicitly set android:exported=\"false\".",
                ))

    return issues


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--variant", choices=("diagnostic", "companion"), required=True)
    parser.add_argument("--manifest", type=Path, required=True, help="Decoded XML from apkanalyzer manifest print")
    args = parser.parse_args(argv)

    try:
        manifest_xml = args.manifest.read_text(encoding="utf-8")
    except OSError as error:
        print(f"ERROR: cannot read manifest {args.manifest}: {error}", file=sys.stderr)
        return 2

    issues = audit_manifest(manifest_xml, args.variant)
    if issues:
        for issue in issues:
            print(f"FAIL [{issue.code}] {issue.message}", file=sys.stderr)
        return 1

    print(f"PASS: {args.variant} APK manifest matches the Relogio 2.0 security policy.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
