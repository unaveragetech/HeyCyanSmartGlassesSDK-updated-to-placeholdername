@echo off
REM 1) Go to sample project directory
cd /d "%~dp0"

REM 2) Build debug APK
echo Building Debug APK...
call gradlew.bat clean assembleDebug
if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

REM 3) Determine built APK path
set APK_PATH=%CD%\app\build\outputs\apk\debug\app-debug.apk
if not exist "%APK_PATH%" (
    echo APK not found: %APK_PATH%
    exit /b 1
)

REM 4) Ensure adb is available
set ADB_PATH=
where adb >nul 2>&1 && set ADB_PATH=adb
if "%ADB_PATH%"=="" (
    if exist "%~dp0\..\..\platform-tools\adb.exe" (
        set ADB_PATH=%~dp0\..\..\platform-tools\adb.exe
    ) else if exist "%~dp0\..\platform-tools\adb.exe" (
        set ADB_PATH=%~dp0\..\platform-tools\adb.exe
    )
)
if "%ADB_PATH%"=="" (
    echo adb not found in path and local platform-tools not found. Please install Platform Tools and add to PATH.
    exit /b 1
)

REM 5) List connected devices
"%ADB_PATH%" devices

REM 6) Uninstall leftover package(s)
set PACKAGE=com.sdk.glassessdksample
echo Uninstalling existing package %PACKAGE% ...
"%ADB_PATH%" uninstall %PACKAGE%

REM 7) Install new build
echo Installing %APK_PATH% ...
"%ADB_PATH%" install -r "%APK_PATH%"
if errorlevel 1 (
    echo Install failed; trying to uninstall all matching packages and retry...
    for /f "tokens=*" %%i in ('"%ADB_PATH%" shell pm list packages ^| findstr /i "%PACKAGE%"') do (
        set "pkg=%%i"
        set "pkg=!pkg:package:=!"
        echo uninstall !pkg!
        "%ADB_PATH%" uninstall !pkg!
    )
    "%ADB_PATH%" install -r "%APK_PATH%"
    if errorlevel 1 (
        echo Retry install failed.
        exit /b 1
    )
)

echo Installed successfully.
exit /b 0
