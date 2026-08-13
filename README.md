# VolumeLockr PLUS

[English](README.md) | [繁體中文](README.zh-TW.md)

VolumeLockr PLUS allows you to control your Android device volume levels and set locks for each one of them.

## Fork Notice & GPLv3 Compliance

This project is a modified fork of the [original VolumeLockr (com.klee.volumelockr)](https://github.com/jonathanklee/VolumeLockr).
The package name has been changed to `com.lnu.volumelockr.plus`.

### Key Modifications

- **Android TV Remote Control & Dynamic Pairing**: Control Android TV / Google TV volume and locks remotely from your phone over Wi-Fi. Features an embedded HTTP server on TV, dynamic 60-second pairing PIN generation, QR code scanning, and persistent token exchange for seamless, passwordless reconnects.
- **Biometric & Password Protection**: Added standalone fingerprint/biometric authentication alongside PBKDF2-hashed password protection with a dedicated security settings page.
- **Modernized UI / UX**:
  - Unified dynamic toggle button for quick "Lock All" / "Unlock All" operations.
  - Direct numeric volume input via tap on value labels.
  - D-pad navigation support and adaptive UI for Android TV.
  - Permission status banner and Do Not Disturb (DND) access alerts.
- **Dynamic Launcher Icon Control**: Support hiding or showing the app icon from the Google TV / Android TV launcher.
- **Localization**: Added Traditional Chinese (zh-TW) and multiple locale translations.

This modified work is released under the same **GNU General Public License v3.0**, complying with open-source copyleft requirements.

---

## Build & Install

### 1. Get the Sources

```bash
git clone git@github.com:ian20040409/VolumeLockr-PLUS.git
```

### 2. Build

```bash
cd VolumeLockr-PLUS
./gradlew assembleDebug
```

### 3. Install

```bash
adb install ./app/build/outputs/apk/debug/app-debug.apk
```
