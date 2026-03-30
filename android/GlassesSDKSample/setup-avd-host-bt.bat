@echo off
setlocal

REM Change this to your AVD name (check with emulator -list-avds)
set "AVD_NAME=Pixel_6"

REM resolve user .android path
set "AVD_HOME=%USERPROFILE%\.android\avd"
set "CONFIG_FILE=%AVD_HOME%\%AVD_NAME%.avd\config.ini"

if not exist "%CONFIG_FILE%" (
  echo [setup-avd-host-bt] ERROR: config.ini not found: %CONFIG_FILE%
  echo Please verify AVD name and path.
  exit /b 1
)

echo [setup-avd-host-bt] Editing: %CONFIG_FILE%

REM set hw.gpu.mode=host and default it to true
echo hw.gpu.mode=host> "%TEMP%\configmod.tmp"
for /f "usebackq delims=" %%I in ("%CONFIG_FILE%") do (
  echo %%I|findstr /b /c:"hw.gpu.mode=" >nul
  if errorlevel 1 (
    echo %%I>>"%TEMP%\configmod.tmp"
  )
)
move /y "%TEMP%\configmod.tmp" "%CONFIG_FILE%" >nul

echo [setup-avd-host-bt] Set hw.gpu.mode=host in %CONFIG_FILE%
echo [setup-avd-host-bt] Please now start emulator and go to Settings -> Bluetooth -> Enable.

endlocal