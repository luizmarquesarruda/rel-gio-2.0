import pathlib
import re
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[2]
KOTLIN_SOURCE = ROOT / "app" / "src" / "main"


class GattCharacteristicReadOnlyPolicyTest(unittest.TestCase):
    def test_application_does_not_write_gatt_characteristics(self):
        write_call = re.compile(r"\.\s*writeCharacteristic\s*\(")
        violations = []

        for source in KOTLIN_SOURCE.rglob("*.kt"):
            content = source.read_text(encoding="utf-8")
            for match in write_call.finditer(content):
                line = content.count("\n", 0, match.start()) + 1
                violations.append(f"{source.relative_to(ROOT)}:{line}")

        self.assertEqual(
            [],
            violations,
            "GATT characteristic writes are forbidden; only READ and CCCD "
            "NOTIFY/INDICATE subscription descriptor writes are allowed.",
        )


if __name__ == "__main__":
    unittest.main()
