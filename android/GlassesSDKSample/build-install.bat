@echo off
setlocal

REM 1) Build apk
echo [build-install] Cleaning and assembling debug APK...
call gradlew.bat clean assembleDebug
if errorlevel 1 (
  echo [build-install] Build failed.
  exit /b 1
)

REM Resolve adb path (use ANDROID_HOME if set, otherwise fallback)
if defined ANDROID_HOME (
    set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
) else (
    set "ADB=C:\Users\b0052\AppData\Local\Android\Sdk\platform-tools\adb.exe"
)

REM 2) Uninstall old package (ignore failure)
echo [build-install] Uninstalling old APK...
"%ADB%" uninstall com.sdk.glassessdksample

REM 3) Install new apk
echo [build-install] Installing new APK...
set "APK_PATH=%cd%\app\build\outputs\apk\debug\app-debug.apk"
echo [build-install] APK path: %APK_PATH%
"%ADB%" install -r -d "%APK_PATH%"
if errorlevel 1 (
  echo [build-install] Install failed.
  exit /b 1
)

REM 4) Launch main activity
echo [build-install] Launching app...
"%ANDROID_HOME%\platform-tools\adb.exe" shell am start -n com.sdk.glassessdksample/.MainActivity

echo [build-install] Done.
endlocal