import importlib.util
from pathlib import Path
import sys
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
ANDROID_NS = "http://schemas.android.com/apk/res/android"
ANDROID = f"{{{ANDROID_NS}}}"

AUDITOR = ROOT / "scripts/audit_apk_manifest.py"
_spec = importlib.util.spec_from_file_location("audit_apk_manifest", AUDITOR)
if _spec is None or _spec.loader is None:
    raise RuntimeError("Could not load APK manifest auditor.")
audit_module = importlib.util.module_from_spec(_spec)
sys.modules[_spec.name] = audit_module
_spec.loader.exec_module(audit_module)


class CurrentAndroidContractTests(unittest.TestCase):
    def test_main_manifest_declares_existing_launcher_icon(self):
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        application = manifest.find("application")
        self.assertIsNotNone(application)

        icon = application.attrib.get(ANDROID + "icon")
        self.assertTrue(icon, "Application must declare android:icon.")
        resource = icon.removeprefix("@")
        self.assertTrue(
            (ROOT / "app/src/main/res" / resource.replace("/", "/", 1) ).exists()
            or (ROOT / "app/src/main/res" / (resource.replace("mipmap/", "mipmap/") + ".xml")).exists(),
            f"Launcher icon resource {icon} must exist.",
        )

    def test_backup_is_explicitly_disabled_for_cloud_and_device_transfer(self):
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        application = manifest.find("application")
        self.assertIsNotNone(application)
        self.assertEqual("false", application.attrib.get(ANDROID + "allowBackup"))
        rules_ref = application.attrib.get(ANDROID + "dataExtractionRules")
        self.assertEqual("@xml/data_extraction_rules", rules_ref)

        rules_path = ROOT / "app/src/main/res/xml/data_extraction_rules.xml"
        self.assertTrue(rules_path.exists(), "Data extraction rules must be checked in.")
        rules = ET.parse(rules_path).getroot()
        self.assertEqual("data-extraction-rules", rules.tag)
        for section_name in ("cloud-backup", "device-transfer"):
            section = rules.find(section_name)
            self.assertIsNotNone(section, f"{section_name} must be explicitly configured.")
            self.assertTrue(
                any(child.tag == "exclude" and child.attrib.get("path") == "."
                    for child in section),
                f"{section_name} must exclude the app's data.",
            )

    def test_android_12_bluetooth_permissions_are_sdk31_scoped(self):
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        permission_names = {
            permission.attrib.get(ANDROID + "name")
            for permission in manifest.findall("uses-permission-sdk-31")
        }
        self.assertIn("android.permission.BLUETOOTH_SCAN", permission_names)
        self.assertIn("android.permission.BLUETOOTH_CONNECT", permission_names)

    def test_manifest_policy_auditor_checks_sdk31_permission_nodes(self):
        xml = f'''<manifest xmlns:android="{ANDROID_NS}"
            package="com.luizmarquesarruda.relogio20.diagnostic">
            <uses-permission-sdk-31 android:name="android.permission.INTERNET" />
            <application android:allowBackup="false"
                android:usesCleartextTraffic="false" />
        </manifest>'''
        codes = {issue.code for issue in audit_module.audit_manifest(xml, "diagnostic")}
        self.assertIn("network_permission_not_approved", codes)

    def test_workflow_records_pr_head_and_tested_merge_commit_separately(self):
        workflow = (ROOT / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")
        self.assertIn("SOURCE_COMMIT:", workflow)
        self.assertIn("github.event.pull_request.head.sha || github.sha", workflow)
        self.assertIn("source_commit=$SOURCE_COMMIT", workflow)
        self.assertIn("tested_commit=$GITHUB_SHA", workflow)

    def test_workflow_uses_current_node24_action_majors(self):
        workflow = (ROOT / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")
        for current in (
            "actions/checkout@v7",
            "actions/setup-java@v6",
            "gradle/actions/setup-gradle@v6",
            "actions/upload-artifact@v7",
        ):
            with self.subTest(action=current):
                self.assertIn(current, workflow)
        for obsolete in (
            "actions/checkout@v4",
            "actions/setup-java@v4",
            "gradle/actions/setup-gradle@v4",
            "actions/upload-artifact@v4",
        ):
            with self.subTest(action=obsolete):
                self.assertNotIn(obsolete, workflow)

    def test_readme_matches_current_read_only_gatt_flow_and_version_source(self):
        readme = (ROOT / "README.md").read_text(encoding="utf-8")
        self.assertNotIn("Permite leitura explícita de características", readme)
        self.assertNotIn("Use **Ler**", readme)
        self.assertNotIn("Use **Ouvir**", readme)
        self.assertNotIn("0.4.0-alpha.2", readme)
        self.assertIn("VERSION", readme)
        self.assertIn("buscar e conectar automaticamente", readme.lower())

    def test_removed_manual_characteristic_read_has_no_dead_callbacks(self):
        source = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/MainActivity.kt"
        ).read_text(encoding="utf-8")
        self.assertNotIn("override fun onCharacteristicRead(", source)
        self.assertNotIn("private fun handleRead(", source)


if __name__ == "__main__":
    unittest.main()
