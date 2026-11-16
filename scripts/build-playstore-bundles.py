#!/usr/bin/env python3
"""
Builds Google Play Store Release Android App Bundles (.aab) or verifies release readiness
for the 3 standalone Quick Delivery apps:
  1. Quick Delivery (Customer)         -> com.aistudio.quickdelivery.ltkeqf
  2. Partner Quick Delivery (Rider)    -> com.aistudio.quickdelivery.ltkeqf.rider
  3. Quick Delivery Admin (Console)    -> com.aistudio.quickdelivery.ltkeqf.admin

Usage:
  python3 scripts/build-playstore-bundles.py --check
  KEYSTORE_PATH=/path/to/my-upload-key.jks STORE_PASSWORD=*** KEY_PASSWORD=*** python3 scripts/build-playstore-bundles.py
"""

import argparse
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "public" / "bundles"

TARGETS = [
    ("CUSTOMER", "QuickDelivery-Customer-release.aab", ""),
    ("RIDER", "QuickDelivery-Rider-Partner-release.aab", ".rider"),
    ("ADMIN", "QuickDelivery-Admin-Enterprise-release.aab", ".admin"),
]


def check_readiness() -> int:
    checks = {
        "google-services.json": (ROOT / "app" / "google-services.json").exists(),
        "firebase_applet_config.xml": (ROOT / "app" / "src" / "main" / "res" / "values" / "firebase_applet_config.xml").exists(),
        "firestore.rules": (ROOT / "firestore.rules").exists(),
        "privacy-policy route": "privacy-policy" in (ROOT / "scripts" / "apk-download-server.mjs").read_text(encoding="utf-8"),
        "proguard-rules.pro": (ROOT / "app" / "proguard-rules.pro").exists(),
    }
    print("Quick Delivery Play Store (.aab) Readiness Report:")
    for label, ok in checks.items():
        print(f"  [{'OK' if ok else 'MISSING'}] {label}")
    keystore = os.environ.get("KEYSTORE_PATH") or str(ROOT / "my-upload-key.jks")
    has_keystore = Path(keystore).exists() and bool(os.environ.get("STORE_PASSWORD")) and bool(os.environ.get("KEY_PASSWORD"))
    print(f"  [{'READY' if has_keystore else 'PENDING_UPLOAD_KEYSTORE'}] Upload Keystore ({keystore})")
    return 0 if all(checks.values()) else 1


def build_bundles() -> int:
    keystore = os.environ.get("KEYSTORE_PATH") or str(ROOT / "my-upload-key.jks")
    if not Path(keystore).exists():
        print(f"Upload keystore not found at {keystore}. Run with --check or set KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD.")
        return 1
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for role, out_name, suffix in TARGETS:
        cmd = ["gradle", ":app:bundleRelease", f"-PappRole={role}", "--no-daemon", "--console=plain"]
        if suffix:
            cmd.append(f"-PappIdSuffix={suffix}")
        print(f"Building {role} Play Store bundle ({out_name})...")
        subprocess.run(cmd, cwd=str(ROOT), check=True)
        src_aab = ROOT / "app" / "build" / "outputs" / "bundle" / "release" / "app-release.aab"
        if not src_aab.exists():
            raise FileNotFoundError(f"Expected bundle not found: {src_aab}")
        dest = OUT_DIR / out_name
        dest.write_bytes(src_aab.read_bytes())
        print(f"Saved {dest} ({dest.stat().st_size} bytes)")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Build or check Play Store .aab bundles.")
    parser.add_argument("--check", action="store_true", help="Verify Play Store release readiness without signing keystore.")
    args = parser.parse_args()
    if args.check:
        return check_readiness()
    return build_bundles()


if __name__ == "__main__":
    sys.exit(main())
