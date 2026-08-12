# VolumeLockr PLUS

[English](README.md) | [繁體中文](README.zh-TW.md)

VolumeLockr PLUS allows you to control your Android device volume levels and set locks for each one of them.

## Fork Notice & GPLv3 Compliance

This project is a modified fork of the [original VolumeLockr (com.klee.volumelockr)](https://github.com/jonathanklee/VolumeLockr).
The package name has been changed to `com.lnu.volumelockr.plus`.

### Key Modifications

- **Wi-Fi Remote Control**: Integrated an HTTP-based remote control feature for adjusting TV volume via a phone.
- **Android TV UI Optimization**: Optimized for Android TV with D-pad support and hidden unnecessary UI elements.
- **Dynamic Launcher Icon Control**: Added ability to hide or show the app icon from the Google TV launcher dynamically.
- **Localization**: Added Traditional Chinese (zh-TW) translation.
- **Logic Enhancements**: Adjusted volume control logic and lock state persistence.

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
