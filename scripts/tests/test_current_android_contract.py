import importlib.util
from pathlib import Path
import sys
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
ANDROID_NS = "http://schemas.android.com/apk/res/android"
ANDROID = f"{{{ANDROID_NS}}}"
TOOLS_NS = "http://schemas.android.com/tools"
TOOLS = f"{{{TOOLS_NS}}}"

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

    def test_android_12_bluetooth_permissions_use_valid_permission_elements(self):
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        permissions = {
            permission.attrib.get(ANDROID + "name"): permission
            for permission in manifest.findall("uses-permission")
        }
        self.assertIn("android.permission.BLUETOOTH_SCAN", permissions)
        self.assertIn("android.permission.BLUETOOTH_CONNECT", permissions)
        scan_permission = permissions["android.permission.BLUETOOTH_SCAN"]
        self.assertEqual("neverForLocation", scan_permission.attrib.get(ANDROID + "usesPermissionFlags"))
        self.assertEqual("s", scan_permission.attrib.get(TOOLS + "targetApi"))
        self.assertIsNone(manifest.find("uses-permission-sdk-31"))

    def test_weather_network_permissions_are_scoped_and_declared_once(self):
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        names = [
            permission.attrib.get(ANDROID + "name")
            for permission in manifest.findall("uses-permission")
        ]
        self.assertEqual(1, names.count("android.permission.INTERNET"))
        self.assertEqual(1, names.count("android.permission.ACCESS_NETWORK_STATE"))
        legacy_location_permissions = {
            permission.attrib.get(ANDROID + "name"): permission
            for permission in manifest.findall("uses-permission")
            if permission.attrib.get(ANDROID + "name") in {
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
            }
        }
        for name, permission in legacy_location_permissions.items():
            with self.subTest(permission=name):
                max_sdk = permission.attrib.get(ANDROID + "maxSdkVersion")
                self.assertIsNotNone(max_sdk)
                self.assertLessEqual(int(max_sdk), 30)

    def test_all_characteristic_writes_are_forbidden(self):
        source = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/MainActivity.kt"
        ).read_text(encoding="utf-8")
        policy = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/device/ble/gatt/AccessPolicy.kt"
        ).read_text(encoding="utf-8")
        self.assertNotIn("writeCharacteristic(", source)
        self.assertNotIn('makeButton("Sincronizar hora")', source)
        self.assertIn("allowWrite: Set<UUID> = emptySet()", policy)
        self.assertIn("fun canWrite(", policy)
        self.assertIn("return false", policy)

    def test_spontaneous_disconnect_records_pending_gatt_discovery_interruption(self):
        source = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/MainActivity.kt"
        ).read_text(encoding="utf-8")
        disconnected = source.split(
            "else if (newState == BluetoothProfile.STATE_DISCONNECTED)", 1
        )[1].split("override fun onServicesDiscovered", 1)[0]
        self.assertIn("recordPendingGattDiscoveryInterruption", disconnected)
        self.assertLess(
            disconnected.index("recordPendingGattDiscoveryInterruption"),
            disconnected.index("cancelGattServiceDiscoveryTimeout"),
        )

    def test_diagnostic_export_does_not_block_activity_or_export_raw_logs(self):
        source = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/MainActivity.kt"
        ).read_text(encoding="utf-8")
        destroy = source.split("override fun onDestroy()", 1)[1]
        self.assertNotIn("flushPendingWrites()", destroy)
        export = source.split("private fun exportDiagnosticReport()", 1)[1].split(
            "private fun appendLog", 1
        )[0]
        self.assertIn("withContext(Dispatchers.IO)", export)
        self.assertNotIn("logLines.toList()", export)

    def test_pending_gatt_interruption_uses_async_persistence(self):
        source = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/MainActivity.kt"
        ).read_text(encoding="utf-8")
        interruption = source.split("private fun recordPendingGattDiscoveryInterruption", 1)[1].split(
            "@SuppressLint", 1
        )[0]
        self.assertNotIn("flushPendingWrites()", interruption)
        self.assertIn("recordDiscoveryAsync(", interruption)


    def test_payloads_without_an_active_capture_are_skipped_not_failed(self):
        store = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/data/discovery/DiscoveryLibraryStore.kt"
        ).read_text(encoding="utf-8")
        self.assertIn("if (activeSession(nowMillis) == null) return true", store)
        self.assertIn("): Boolean? = synchronized(STORE_LOCK)", store)
        self.assertIn("if (operation() == false) failedWrites.incrementAndGet()", store)

    def test_activity_session_start_and_finish_use_ordered_async_writer(self):
        main = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/MainActivity.kt"
        ).read_text(encoding="utf-8")
        feature = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/FeatureDiscoveryActivity.kt"
        ).read_text(encoding="utf-8")
        for source in (main, feature):
            self.assertNotIn("startTestSession(", source)
            self.assertNotIn("finishTestSession(", source)
            self.assertIn("startTestSessionAsync", source)
            self.assertIn("finishTestSessionAsync", source)


    def test_ktlint_gate_requires_successful_gradle_setup(self):
        workflow = (ROOT / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")
        self.assertIn("steps.manifest_policy_tests.outcome == 'success'", workflow)
        self.assertIn("steps.setup_gradle.outcome == 'success'", workflow)
        self.assertIn("steps.ktlint_baseline_bootstrap.outcome == 'skipped'", workflow)

    def test_manual_screens_apply_shared_edge_to_edge_insets(self):
        ui = (ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/RelogioUi.kt").read_text(encoding="utf-8")
        self.assertIn("WindowCompat.setDecorFitsSystemWindows(activity.window, false)", ui)
        self.assertIn("WindowInsetsCompat.Type.systemBars()", ui)
        for relative_path in (
            "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/MainActivity.kt",
            "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/LocalToolsActivity.kt",
            "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/TimeWeatherActivity.kt",
            "app/src/companion/java/com/luizmarquesarruda/relogio20/notifications/NotificationCenterActivity.kt",
        ):
            self.assertIn("RelogioUi.applyEdgeToEdge", (ROOT / relative_path).read_text(encoding="utf-8"))

    def test_local_health_reminder_is_restored_after_reboot(self):
        manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        source = (ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/LocalToolsActivity.kt").read_text(encoding="utf-8")
        self.assertIn("android.permission.RECEIVE_BOOT_COMPLETED", manifest)
        self.assertIn("android.intent.action.BOOT_COMPLETED", manifest)
        self.assertIn("Intent.ACTION_BOOT_COMPLETED", source)
        self.assertIn("Intent.ACTION_MY_PACKAGE_REPLACED", source)
        self.assertIn("setInexactRepeating", source)

    def test_time_weather_screen_is_internal_and_reachable_from_feature_hub(self):
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        application = manifest.find("application")
        activity = next(
            node for node in application.findall("activity")
            if node.attrib.get(ANDROID + "name") == ".ui.screens.TimeWeatherActivity"
        )
        self.assertEqual("false", activity.attrib.get(ANDROID + "exported"))
        feature_hub = (ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/FeatureHubActivity.kt").read_text(encoding="utf-8")
        self.assertIn('Feature("Hora e clima"', feature_hub)
        self.assertIn('"time_weather" -> startActivity(Intent(this, TimeWeatherActivity::class.java))', feature_hub)

    def test_manifest_policy_auditor_rejects_sdk31_permission_element(self):
        xml = f'''<manifest xmlns:android="{ANDROID_NS}"
            package="com.luizmarquesarruda.relogio20.diagnostic">
            <uses-permission-sdk-31 android:name="android.permission.INTERNET" />
            <application android:allowBackup="false"
                android:usesCleartextTraffic="false" />
        </manifest>'''
        codes = {issue.code for issue in audit_module.audit_manifest(xml, "diagnostic")}
        self.assertIn("unsupported_permission_element", codes)

    def test_workflow_records_pr_head_and_tested_merge_commit_separately(self):
        workflow = (ROOT / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")
        self.assertIn("SOURCE_COMMIT:", workflow)
        self.assertIn("github.event.pull_request.head.sha || github.sha", workflow)
        self.assertIn("source_commit=$SOURCE_COMMIT", workflow)
        self.assertIn("tested_commit=$GITHUB_SHA", workflow)

    def test_toolchain_uses_fully_supported_modern_versions(self):
        root_build = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")
        workflow = (ROOT / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")
        self.assertIn('id("com.android.application") version "8.13.2"', root_build)
        self.assertIn('id("org.jetbrains.kotlin.android") version "2.4.21"', root_build)
        self.assertIn('gradle-version: "8.13"', workflow)
        self.assertNotIn('gradle-version: "9.5.0"', workflow)
        app_build = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('androidx.work:work-runtime-ktx:2.12.0', app_build)

    def test_gatt_api33_helper_uses_requires_api_and_preferences_use_ktx(self):
        source = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/MainActivity.kt"
        ).read_text(encoding="utf-8")
        self.assertIn("@RequiresApi(Build.VERSION_CODES.TIRAMISU)", source)
        self.assertNotIn("@TargetApi", source)
        self.assertIn("import androidx.core.content.edit", source)
        self.assertNotIn("identityPrefs.edit()", source)


    def test_diagnostic_apk_artifact_contains_only_the_apk(self):
        workflow = (ROOT / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")
        apk_publish = workflow.split("- name: Publish diagnostic APK only", 1)[1].split(
            "- name: Publish Android validation reports", 1
        )[0]
        reports_publish = workflow.split(
            "- name: Publish Android validation reports", 1
        )[1]

        self.assertIn("name: relogio-2.0-diagnostic-debug-apk", apk_publish)
        self.assertIn(
            "path: app/build/outputs/apk/diagnostic/debug/app-diagnostic-debug.apk",
            apk_publish,
        )
        self.assertNotIn("build/distributions", apk_publish)
        self.assertNotIn(".txt", apk_publish)
        self.assertNotIn(".xml", apk_publish)

        self.assertIn("name: android-validation-reports", reports_publish)
        self.assertIn("app/build/reports/security/**", reports_publish)
        self.assertIn('cat > "$REPORT_DIR/diagnostic-build-info.txt"', workflow)
        self.assertNotIn('cat > "$PACKAGE_DIR/BUILD-INFO.txt"', workflow)
        self.assertIn(
            "group: android-ci-${{ github.event.pull_request.number || github.ref_name }}",
            workflow,
        )
        self.assertNotIn("group: android-ci-${{ github.event.pull_request.number || github.ref }}", workflow)

    def test_ci_runs_once_for_feature_prs_and_stops_after_cancellation(self):
        workflow = (ROOT / ".github/workflows/android-ci.yml").read_text(encoding="utf-8")
        triggers = workflow.split("concurrency:", 1)[0]
        self.assertIn("pull_request:", triggers)
        self.assertIn("push:", triggers)
        push_triggers = triggers.split("pull_request:", 1)[0]
        pull_request_triggers = triggers.split("pull_request:", 1)[1]
        self.assertIn("      - main", push_triggers)
        self.assertNotIn("      - 'fix/**'", push_triggers)
        self.assertIn("      - 'fix/**'", pull_request_triggers)

        for step in (
            "Run unit tests and assemble both variants",
            "Enforce privacy dependency and permission guardrails",
            "Audit merged APK manifests, signatures and hashes",
            "Prepare diagnostic APK metadata in the reports artifact",
            "Publish Android validation reports",
        ):
            with self.subTest(step=step):
                start = workflow.index("- name: " + step)
                next_step = workflow.find("\n      - name:", start + 1)
                block = workflow[start:] if next_step < 0 else workflow[start:next_step]
                self.assertIn("!cancelled()", block)

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
        self.assertNotIn("Conecta ao dispositivo escolhido", readme)
        self.assertIn("não é necessário digitar o endereço do relógio", readme)
        self.assertIn("O endereço salvo é uma pista, não um filtro exclusivo", readme)
        self.assertNotIn("Permite leitura explícita de características", readme)
        self.assertNotIn("Use **Ler**", readme)
        self.assertNotIn("Use **Ouvir**", readme)
        self.assertNotIn("0.4.0-alpha.2", readme)
        self.assertIn("VERSION", readme)
        self.assertIn("buscar e conectar automaticamente", readme.lower())

    def test_gatt_read_callbacks_are_present_for_serialized_exploration(self):
        source = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/MainActivity.kt"
        ).read_text(encoding="utf-8")
        self.assertIn("override fun onCharacteristicRead(", source)
        self.assertIn("completeGattOperation(", source)
        self.assertNotIn("private fun handleRead(", source)
        self.assertIn("GATT_OPERATION_TIMEOUT_MS", source)
        self.assertIn("private fun runDeepGattExploration(", source)




    def test_dashboard_is_launcher_with_six_main_level_secondary_areas(self):
        manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        android = "{" + ANDROID_NS + "}"
        application = manifest.find("application")
        self.assertIsNotNone(application)
        activities = {
            node.attrib.get(android + "name"): node
            for node in application.findall("activity")
        }
        expected = {
            ".ui.screens.MainActivity",
            ".ui.screens.HealthOverviewActivity",
            ".ui.screens.ActivityOverviewActivity",
            ".ui.screens.SleepOverviewActivity",
            ".ui.screens.HistoryActivity",
            ".ui.screens.MoreResourcesActivity",
        }
        self.assertTrue(expected.issubset(set(activities)))
        self.assertIn(".ui.screens.SettingsActivity", activities)
        dashboard = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/DashboardActivity.kt"
        ).read_text(encoding="utf-8")
        self.assertEqual(6, dashboard.count('"TELA '))
        self.assertNotIn("SettingsActivity::class.java", dashboard)
        self.assertEqual("false", activities[".ui.screens.MainActivity"].attrib.get(android + "exported"))
        launchers = []
        for name, node in activities.items():
            for intent in node.findall("intent-filter"):
                actions = {item.attrib.get(android + "name") for item in intent.findall("action")}
                categories = {item.attrib.get(android + "name") for item in intent.findall("category")}
                if "android.intent.action.MAIN" in actions and "android.intent.category.LAUNCHER" in categories:
                    launchers.append(name)
        self.assertEqual([".ui.screens.DashboardActivity"], launchers)

    def test_settings_screen_is_nested_under_more_resources_and_preferences_are_shared(self):
        settings = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/SettingsActivity.kt"
        ).read_text(encoding="utf-8")
        preferences = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/data/preferences/AppPreferences.kt"
        ).read_text(encoding="utf-8")
        more_resources = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/MoreResourcesActivity.kt"
        ).read_text(encoding="utf-8")
        local_tools = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/LocalToolsActivity.kt"
        ).read_text(encoding="utf-8")
        dashboard = (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/DashboardActivity.kt"
        ).read_text(encoding="utf-8")
        self.assertIn("AppPreferences.set24HourClock", settings)
        self.assertIn("AppPreferences.setKilometers", settings)
        self.assertIn("AppPreferences.setIncludeExternalHealthSources", settings)
        self.assertIn("Context.MODE_PRIVATE", preferences)
        self.assertIn("SettingsActivity::class.java", more_resources)
        self.assertNotIn("SettingsActivity::class.java", dashboard)
        self.assertIn("AppPreferences.setIncludeExternalHealthSources", local_tools)
        self.assertIn("AppPreferences.includesExternalHealthSources", local_tools)
        self.assertIn("LEGACY_KEY_INCLUDE_EXTERNAL", preferences)
        self.assertIn("HealthConnectOriginPolicy.INCLUDE_EXTERNAL", (
            ROOT / "app/src/main/java/com/luizmarquesarruda/relogio20/ui/screens/ActivityOverviewActivity.kt"
        ).read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
