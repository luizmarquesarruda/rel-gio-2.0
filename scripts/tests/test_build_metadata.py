from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]


class BuildMetadataTests(unittest.TestCase):
    def test_version_file_matches_android_default_version_name(self):
        version = (ROOT / "VERSION").read_text(encoding="utf-8").strip()
        gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
        match = re.search(
            r'^\\s*versionName\\s*=\\s*"([^"]+)"',
            gradle,
            flags=re.MULTILINE,
        )

        self.assertIsNotNone(match, "Android defaultConfig must define versionName.")
        self.assertEqual(version, match.group(1))

    def test_diagnostic_artifact_uses_canonical_version_dynamically(self):
        workflow = (ROOT / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")

        self.assertIn('APP_VERSION="$(tr -d', workflow)
        self.assertIn("version=${APP_VERSION}-diagnostic", workflow)
        self.assertNotIn("version=0.4.0-alpha.2-diagnostic", workflow)


if __name__ == "__main__":
    unittest.main()
