# VolumeLockr PLUS

[English](README.md) | [繁體中文](README.zh-TW.md)

VolumeLockr PLUS 是一款用於控制 Android 裝置音量並為各個音量頻道設置鎖定狀態的應用程式。

## Fork 說明與 GPLv3 條款聲明

本專案為 [原始 VolumeLockr (com.klee.volumelockr)](https://github.com/jonathanklee/VolumeLockr) 的修改分支版本。
應用程式套件名稱已調整為 `com.lnu.volumelockr.plus`。

### 主要修改項目

- **Wi-Fi 遠端控制**：整合基於 HTTP 的遠端控制功能，支援透過手機網路調整 TV 音量。
- **Android TV UI 優化**：支援 D-pad 遙控器操作，並針對大螢幕隱藏不必要的介面元素。
- **動態隱藏圖示**：支援從 Google TV 啟動器中動態隱藏或顯示應用程式圖示。
- **多語言支援**：新增繁體中文 (zh-TW) 介面翻譯。
- **邏輯優化**：調整音量控制邏輯與鎖定狀態的持久化儲存機制。

本修改衍生作品依據 **GNU General Public License v3.0** 授權條款釋出，完整遵守開源 Copyleft 之要求。

---

## 手動編譯與安裝

### 1. 取得原始碼

```bash
git clone git@github.com:ian20040409/VolumeLockr-PLUS.git
```

### 2. 編譯 APK

```bash
cd VolumeLockr-PLUS
./gradlew assembleDebug
```

### 3. 安裝至裝置

```bash
adb install ./app/build/outputs/apk/debug/app-debug.apk
```
