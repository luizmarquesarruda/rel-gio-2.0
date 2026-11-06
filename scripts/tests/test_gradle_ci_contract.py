from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]


class GradleCiContractTests(unittest.TestCase):
    def test_ci_gradle_version_matches_agp_813_line(self):
        workflow = (ROOT / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")
        root_build = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")

        self.assertIn('id("com.android.application") version "8.13.2"', root_build)
        self.assertRegex(workflow, r'gradle-version:\s*"8\.13"')
        self.assertNotRegex(workflow, r'gradle-version:\s*"9\.')

    def test_bootstrap_and_regular_build_do_not_repeat_in_same_job(self):
        workflow = (ROOT / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")
        build_step = re.search(
            r"- name: Run unit tests and assemble both variants(?P<body>.*?)(?=\n      - name:)",
            workflow,
            re.S,
        )
        self.assertIsNotNone(build_step)
        self.assertIn("steps.dependency_verification_bootstrap.outcome == 'skipped'", build_step.group("body"))

    def test_downstream_apk_gates_accept_successful_bootstrap_build(self):
        workflow = (ROOT / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")
        self.assertGreaterEqual(
            workflow.count("steps.dependency_verification_bootstrap.outcome == 'success'"),
            3,
        )


if __name__ == "__main__":
    unittest.main()
