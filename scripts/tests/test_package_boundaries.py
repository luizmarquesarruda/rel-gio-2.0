from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20"


class PackageBoundaryTests(unittest.TestCase):
    def test_activities_live_in_ui_screens(self):
        files = (
            "ui/screens/MainActivity.kt",
            "ui/screens/FeatureHubActivity.kt",
            "ui/screens/FeatureDiscoveryActivity.kt",
            "ui/screens/DiscoveryLibraryActivity.kt",
            "ui/screens/HealthPrivacyActivity.kt",
        )
        for relative in files:
            with self.subTest(file=relative):
                source = (SOURCE / relative).read_text(encoding="utf-8")
                self.assertTrue(
                    source.startswith("package com.luizmarquesarruda.relogio20.ui.screens"),
                    relative,
                )

    def test_pure_helpers_live_in_their_responsibility_packages(self):
        expected = {
            "device/ble/core/BleDeviceIdentity.kt": "com.luizmarquesarruda.relogio20.device.ble.core",
            "device/ble/core/BleScanCandidatePolicy.kt": "com.luizmarquesarruda.relogio20.device.ble.core",
            "device/ble/core/BleValueFormatter.kt": "com.luizmarquesarruda.relogio20.device.ble.core",
            "domain/diagnostics/BleDiagnosticReport.kt": "com.luizmarquesarruda.relogio20.domain.diagnostics",
            "ui/formatters/StepsCountFormatter.kt": "com.luizmarquesarruda.relogio20.ui.formatters",
        }
        for relative, package in expected.items():
            with self.subTest(file=relative):
                source = (SOURCE / relative).read_text(encoding="utf-8")
                self.assertTrue(source.startswith(f"package {package}\n"), relative)

    def test_old_root_source_paths_are_not_left_behind(self):
        legacy = (
            "MainActivity.kt",
            "FeatureHubActivity.kt",
            "FeatureDiscoveryActivity.kt",
            "DiscoveryLibraryActivity.kt",
            "HealthPrivacyActivity.kt",
            "BleDeviceIdentity.kt",
            "BleScanCandidatePolicy.kt",
            "BleValueFormatter.kt",
            "BleDiagnosticReport.kt",
            "StepsCountFormatter.kt",
        )
        for filename in legacy:
            with self.subTest(file=filename):
                self.assertFalse((SOURCE / filename).exists(), filename)

    def test_manifest_points_to_the_moved_activity_classes(self):
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        android = "{http://schemas.android.com/apk/res/android}"
        application = manifest.find("application")
        self.assertIsNotNone(application)
        activities = {
            node.attrib.get(android + "name")
            for node in application.findall("activity")
        }
        for class_name in (
            ".ui.screens.MainActivity",
            ".ui.screens.FeatureHubActivity",
            ".ui.screens.FeatureDiscoveryActivity",
            ".ui.screens.DiscoveryLibraryActivity",
            ".ui.screens.HealthPrivacyActivity",
        ):
            with self.subTest(activity=class_name):
                self.assertIn(class_name, activities)
        alias = next(
            node for node in application.findall("activity-alias")
            if node.attrib.get(android + "name") == ".HealthPrivacyPermissionUsageActivity"
        )
        self.assertEqual(".ui.screens.HealthPrivacyActivity", alias.attrib.get(android + "targetActivity"))


if __name__ == "__main__":
    unittest.main()
