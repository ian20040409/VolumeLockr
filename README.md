# VolumeLockr

> **Fork Notice & GPLv3 Compliance:**
> This project is a modified fork of the original VolumeLockr (com.klee.volumelockr).
> The package name has been changed to `com.lnu.volumelockr`.
> 
> **Key Modifications:**
> - Integrated an HTTP-based Wi-Fi Remote Control feature for adjusting TV volume via a phone.
> - Optimized UI for Android TV, including D-pad support and hiding specific elements.
> - Added ability to hide the TV app icon from the Google TV launcher dynamically.
> - Added Traditional Chinese (zh-TW) translation.
> - Adjusted volume logic and lock state persistence.
> 
> This modified work is released under the same **GNU General Public License v3.0**, complying with open-source copyleft requirements.

 
VolumeLockr allows you to control your Android device volume levels and set locks for each one of them.

    
### Build manually
#### Get the sources

```
git clone git@github.com:jonathanklee/VolumeLockr.git
```

#### Build
```
cd VolumeLockr
./gradlew assembleDebug
````
#### Install
```
adb install ./app/build/outputs/apk/debug/app-debug.apk
```
