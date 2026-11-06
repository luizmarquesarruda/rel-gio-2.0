import json
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]


class DeveloperToolingTests(unittest.TestCase):
    def test_leakcanary_is_limited_to_diagnostic_debug(self):
        gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
        guardrails = (ROOT / "scripts/check-privacy-guardrails.sh").read_text(encoding="utf-8")

        self.assertIn(
            'diagnosticDebugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")',
            gradle,
        )
        self.assertNotRegex(gradle, r'(?m)^\s*(?:implementation|debugImplementation)\('
                            r'"com\.squareup\.leakcanary:')
        self.assertIn("diagnosticReleaseRuntimeClasspath", guardrails)
        self.assertIn("companionDebugRuntimeClasspath", guardrails)
        self.assertIn("companionBenchmarkRuntimeClasspath", guardrails)
        self.assertIn("LeakCanary must not be present", guardrails)

    def test_macrobenchmark_targets_only_diagnostic_flavor(self):
        settings = (ROOT / "settings.gradle.kts").read_text(encoding="utf-8")
        app_gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
        benchmark_gradle = (ROOT / "macrobenchmark/build.gradle.kts").read_text(
            encoding="utf-8"
        )
        manifest = (ROOT / "app/src/diagnosticBenchmark/AndroidManifest.xml").read_text(
            encoding="utf-8"
        )
        main_manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(
            encoding="utf-8"
        )

        self.assertIn('include(":app", ":macrobenchmark")', settings)
        self.assertIn('create("benchmark")', app_gradle)
        self.assertIn('diagnosticBenchmarkImplementation('
                      '"androidx.profileinstaller:profileinstaller:1.4.1")', app_gradle)
        self.assertIn('missingDimensionStrategy("distribution", "diagnostic")', benchmark_gradle)
        self.assertIn('variantBuilder.buildType == "benchmark"', benchmark_gradle)
        self.assertIn('targetProjectPath = ":app"', benchmark_gradle)
        self.assertIn('android:shell="true"', manifest)
        self.assertNotIn("<profileable", main_manifest)

    def test_renovate_never_merges_automatically_and_waits_for_stability(self):
        config = json.loads((ROOT / "renovate.json").read_text(encoding="utf-8"))

        self.assertIs(config["automerge"], False)
        self.assertTrue(config["dependencyDashboard"])
        self.assertEqual(config["minimumReleaseAge"], "3 days")
        self.assertEqual(config["internalChecksFilter"], "strict")
        managers = config["customManagers"]
        self.assertEqual(len(managers), 1)
        self.assertEqual(managers[0]["datasourceTemplate"], "gradle-version")
        self.assertEqual(managers[0]["depNameTemplate"], "gradle")

    def test_macrobenchmark_measures_cold_startup_without_emulator_ci(self):
        source = (
            ROOT
            / "macrobenchmark/src/main/java/com/luizmarquesarruda/relogio20/"
            "macrobenchmark/RelogioStartupMacrobenchmark.kt"
        ).read_text(encoding="utf-8")
        workflow = (ROOT / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")

        self.assertIn("StartupTimingMetric()", source)
        self.assertIn("StartupMode.COLD", source)
        self.assertIn("iterations = 10", source)
        self.assertIn(":macrobenchmark:assemble", workflow)
        self.assertNotIn(":macrobenchmark:connectedCheck", workflow)

    def test_dependency_verification_metadata_is_sha256_pinned_when_present(self):
        metadata = ROOT / "gradle/verification-metadata.xml"
        if not metadata.exists():
            self.skipTest("Metadata será anexado após revisar o artefato de bootstrap da CI.")

        tree = ET.parse(metadata)
        root = tree.getroot()
        ns = {"g": "https://schema.gradle.org/dependency-verification"}
        artifacts = root.findall(".//g:artifact", ns)
        self.assertGreater(len(artifacts), 0, "Metadata não pode estar vazia.")

        for artifact in artifacts:
            self.assertIsNotNone(
                artifact.find("g:sha256", ns),
                f"Artefato sem SHA-256: {artifact.attrib}",
            )
            self.assertIsNone(artifact.find("g:sha1", ns))
            self.assertIsNone(artifact.find("g:md5", ns))


if __name__ == "__main__":
    unittest.main()
