import importlib.util
from pathlib import Path
import sys
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "audit_apk_manifest.py"
_spec = importlib.util.spec_from_file_location("audit_apk_manifest", SCRIPT)
if _spec is None or _spec.loader is None:
    raise RuntimeError("Could not load audit_apk_manifest.py")
audit_module = importlib.util.module_from_spec(_spec)
sys.modules[_spec.name] = audit_module
_spec.loader.exec_module(audit_module)
audit_manifest = audit_module.audit_manifest

ANDROID = "http://schemas.android.com/apk/res/android"


def manifest(permissions="", services="", app_attrs='android:allowBackup="false" android:usesCleartextTraffic="false" android:icon="@drawable/ic_launcher" android:dataExtractionRules="@xml/data_extraction_rules" android:fullBackupContent="@xml/backup_rules"', package_name="com.luizmarquesarruda.relogio20.diagnostic", include_health_rationale=True):
    rationale = ""
    if include_health_rationale:
        rationale = """
        <activity android:name=".HealthPrivacyActivity" android:exported="true">
          <intent-filter>
            <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
          </intent-filter>
        </activity>
        <activity-alias android:name=".HealthPrivacyPermissionUsageActivity"
          android:exported="true" android:targetActivity=".HealthPrivacyActivity"
          android:permission="android.permission.START_VIEW_PERMISSION_USAGE">
          <intent-filter>
            <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
            <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
          </intent-filter>
        </activity-alias>
        """
    return f'''<?xml version="1.0" encoding="utf-8"?>
    <manifest xmlns:android="{ANDROID}" package="{package_name}">
      {permissions}
      <application {app_attrs}>{rationale}{services}</application>
    </manifest>'''


class DiagnosticVariantTests(unittest.TestCase):
    def test_diagnostic_without_notification_listener_is_accepted(self):
        xml = manifest(
            permissions='''<uses-permission android:name="android.permission.health.READ_STEPS" />
              <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
              <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />'''
        )
        self.assertEqual([], audit_manifest(xml, "diagnostic"))

    def test_diagnostic_rejects_notification_listener_service(self):
        xml = manifest(services='''<service android:name=".notifications.NotificationCaptureService"
          android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
          android:exported="false"><intent-filter><action
          android:name="android.service.notification.NotificationListenerService" />
          </intent-filter></service>''')
        codes = {issue.code for issue in audit_manifest(xml, "diagnostic")}
        self.assertIn("diagnostic_notification_listener_present", codes)


class CompanionVariantTests(unittest.TestCase):
    def test_companion_requires_protected_non_exported_notification_listener(self):
        xml = manifest(services='''<service android:name=".notifications.NotificationCaptureService"
          android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
          android:exported="false"><intent-filter><action
          android:name="android.service.notification.NotificationListenerService" />
          </intent-filter></service>''', package_name="com.luizmarquesarruda.relogio20")
        self.assertEqual([], audit_manifest(xml, "companion"))

    def test_companion_rejects_missing_notification_listener(self):
        xml = manifest(package_name="com.luizmarquesarruda.relogio20")
        codes = {issue.code for issue in audit_manifest(xml, "companion")}
        self.assertIn("companion_notification_listener_missing", codes)

    def test_companion_rejects_unprotected_notification_listener(self):
        xml = manifest(services='''<service android:name=".notifications.NotificationCaptureService"
          android:exported="false"><intent-filter><action
          android:name="android.service.notification.NotificationListenerService" />
          </intent-filter></service>''', package_name="com.luizmarquesarruda.relogio20")
        codes = {issue.code for issue in audit_manifest(xml, "companion")}
        self.assertIn("notification_listener_permission_missing", codes)

    def test_companion_rejects_exported_notification_listener(self):
        xml = manifest(services='''<service android:name=".notifications.NotificationCaptureService"
          android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
          android:exported="true"><intent-filter><action
          android:name="android.service.notification.NotificationListenerService" />
          </intent-filter></service>''', package_name="com.luizmarquesarruda.relogio20")
        codes = {issue.code for issue in audit_manifest(xml, "companion")}
        self.assertIn("notification_listener_must_not_be_exported", codes)


class SharedSecurityPolicyTests(unittest.TestCase):
    def test_health_connect_privacy_rationale_entry_points_are_accepted(self):
        xml = manifest()
        for variant in ("diagnostic", "companion"):
            with self.subTest(variant=variant):
                self.assertEqual([], audit_manifest(xml, variant))

    def test_both_variants_reject_missing_health_connect_privacy_rationale(self):
        xml = manifest(include_health_rationale=False)
        for variant in ("diagnostic", "companion"):
            with self.subTest(variant=variant):
                codes = {issue.code for issue in audit_manifest(xml, variant)}
                self.assertIn("health_permission_rationale_activity_missing", codes)
                self.assertIn("health_permission_usage_alias_missing", codes)

    def test_apkanalyzer_raw_ampersand_in_label_is_normalized(self):
        xml = manifest(app_attrs='android:allowBackup="false" android:usesCleartextTraffic="false" android:icon="@drawable/ic_launcher" android:dataExtractionRules="@xml/data_extraction_rules" android:fullBackupContent="@xml/backup_rules" android:label="Saúde & Fitness"')
        self.assertEqual([], audit_manifest(xml, "diagnostic"))

    def test_valid_xml_ampersand_entity_is_preserved(self):
        xml = manifest(app_attrs='android:allowBackup="false" android:usesCleartextTraffic="false" android:icon="@drawable/ic_launcher" android:dataExtractionRules="@xml/data_extraction_rules" android:fullBackupContent="@xml/backup_rules" android:label="Saúde &amp; Fitness"')
        self.assertEqual([], audit_manifest(xml, "diagnostic"))

    def test_both_variants_reject_internet_permission(self):
        xml = manifest(permissions='<uses-permission android:name="android.permission.INTERNET" />')
        for variant in ("diagnostic", "companion"):
            with self.subTest(variant=variant):
                codes = {issue.code for issue in audit_manifest(xml, variant)}
                self.assertIn("network_permission_not_approved", codes)

    def test_both_variants_reject_sms_and_accessibility(self):
        xml = manifest(
            permissions='<uses-permission android:name="android.permission.READ_SMS" />',
            services='''<service android:name=".Reader" android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE" />'''
        )
        for variant in ("diagnostic", "companion"):
            with self.subTest(variant=variant):
                codes = {issue.code for issue in audit_manifest(xml, variant)}
                self.assertIn("sensitive_permission_not_approved", codes)
                self.assertIn("accessibility_service_not_approved", codes)

    def test_location_permission_must_be_limited_to_legacy_android(self):
        xml = manifest(permissions='<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />')
        codes = {issue.code for issue in audit_manifest(xml, "diagnostic")}
        self.assertIn("legacy_location_permission_not_limited", codes)

    def test_variant_package_id_must_match_distribution_target(self):
        xml = manifest(package_name="com.unrelated.app")
        codes = {issue.code for issue in audit_manifest(xml, "diagnostic")}
        self.assertIn("variant_package_id_mismatch", codes)

    def test_backup_must_be_disabled(self):
        xml = manifest(app_attrs='android:allowBackup="true"')
        codes = {issue.code for issue in audit_manifest(xml, "diagnostic")}
        self.assertIn("backup_must_be_disabled", codes)

    def test_compiled_resource_references_for_backup_rules_are_accepted(self):
        xml = manifest(app_attrs='android:allowBackup="false" android:usesCleartextTraffic="false" android:icon="@ref/0x7f040006" android:dataExtractionRules="@ref/0x7f0b0001" android:fullBackupContent="@ref/0x7f0b0000"')
        self.assertEqual([], audit_manifest(xml, "diagnostic"))

    def test_missing_data_extraction_rules_are_rejected(self):
        xml = manifest(app_attrs='android:allowBackup="false" android:usesCleartextTraffic="false" android:icon="@drawable/ic_launcher" android:fullBackupContent="@xml/backup_rules"')
        codes = {issue.code for issue in audit_manifest(xml, "diagnostic")}
        self.assertIn("data_extraction_rules_missing", codes)

    def test_missing_legacy_backup_rules_are_rejected(self):
        xml = manifest(app_attrs='android:allowBackup="false" android:usesCleartextTraffic="false" android:icon="@drawable/ic_launcher" android:dataExtractionRules="@xml/data_extraction_rules"')
        codes = {issue.code for issue in audit_manifest(xml, "diagnostic")}
        self.assertIn("legacy_backup_rules_missing", codes)

    def test_cleartext_traffic_must_not_be_enabled(self):
        xml = manifest(app_attrs='android:allowBackup="false" android:usesCleartextTraffic="true"')
        codes = {issue.code for issue in audit_manifest(xml, "diagnostic")}
        self.assertIn("cleartext_traffic_enabled", codes)


if __name__ == "__main__":
    unittest.main()
