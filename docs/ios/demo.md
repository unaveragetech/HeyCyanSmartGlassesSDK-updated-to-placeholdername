# iOS Demo App Walkthrough

This document explains how the iOS demo app (`ios/QCSDKDemo`) uses the SDK APIs and manages the device flows.

## Main structure
- `ViewController` is the central dashboard with actions:
  - Get device version / firmware
  - Set time
  - Get battery
  - Get media info
  - Take photo
  - Start/stop video
  - Start/stop audio
  - Take AI photo (AI image generation)
  - Switch to capture or transfer mode
  - Download media over Wi-Fi
  - View media gallery

- `QCCentralManager` manages BLE scanning and connection state
- `GlassesWiFiHandler` manages Wi-Fi hotspot join via NEHotspotConfiguration
- `GlassesMediaDownloader` orchestrates `openWifiWithMode` + `getDeviceWifiIP` + auto/manual Wi-Fi join + `/files/media.config` download and media retrieval
- `MediaGalleryViewController` presents local media view (from `Documents/GlassesMedia`)

## Scan and pair flow
1. User taps `Search` -> `QCScanViewController` appears
2. `QCCentralManager scan` finds peripherals (using `CBUUID` from `QCSDKSERVERUUID1/2`)
3. On selection, `QCCentralManager connect` to selected peripheral
4. `QCCentralManager` adds peripheral to `QCSDKManager` and tracks state
5. Delegates (`didState:`, `didConnected:`, `didDisconnecte:`) update UI in `ViewController`

## Command execution
- `QCSDKCmdCreator` is used for actions (mode, config, info)
- Results are shown in table view and local state
- `didUpdateBatteryLevel`, `didUpdateMediaWithPhotoCount`, `didReceiveAIChatImageData` handle asynchronous updates from `QCSDKManager` delegate

## Capture mode sequence
- `setDeviceMode(QCOperatorDeviceModePhoto)` : take photo
- `setDeviceMode(QCOperatorDeviceModeVideo)` / `VideoStop` : record video
- `setDeviceMode(QCOperatorDeviceModeAudio)` / `AudioStop` : record audio
- `setDeviceMode(QCOperatorDeviceModeAIPhoto)` : AI image
- `switchToCaptureMode` attempts photo mode and falls back to video then photo

## Transfer mode + media download
- `switchToTransferMode` calls `openWifiWithMode(Transfer)`
- On success receives SSID/password
- `GlassesWiFiHandler` / `GlassesMediaDownloader` connect to AP and retrieve `deviceIP`
- The downloader uses `getDeviceWifiIPSuccess` with retries
- After IP is known, it tries `http://deviceIP/files/media.config` and fallback IPs
- It downloads media file list and individual files over HTTP
- If Wi-Fi config fails, fallback to manual instructions and contest retries

## Demo support features
- `Media Gallery`: local media listing from `Documents/GlassesMedia`
- `DeviceStatusCheck`: checks battery, media counts, versions, etc.
- Reconnect semantics in `QCCentralManager` for auto reconnect after disconnect
- Magically handles MAC extraction from advertisement data and bind persistence via NSUserDefaults

## iOS-specific considerations
- NEHotspotConfiguration requires iOS 11+, and on iOS 13+ includes `lifeTimeInDays`
- Use captive network API (`CNCopyCurrentNetworkInfo`) to verify SSID
- Device network may be a non-routed isolated wf; must fetch from direct IP and check progress.
- Attention: HTTP cleartext may require App Transport Security (ATS) exceptions in `Info.plist`

## AI use cases
- `didReceiveAIChatImageData` gets binary image; app sets image preview in cell
- Control modes: translation (`TranslateStart` / `TranslateStop`), speech recognition start/stop can be performed with commands via `QCOperatorDeviceMode`

## Metrics
- `QCCentralManager` emits states: `QCStateUnbind`, `QCStateConnecting`, `QCStateConnected`, `QCStateDisconnecting`, `QCStateDisconnected`
- BLE states: `QCBluetoothStatePoweredOn`, `PoweredOff`, etc.

## Integration notes
- Always verify `isPeripheralFreeNow` before mode switching to avoid Busy status
- Maintain a single connection at time ideally, and unbind on app exit
- Prefer to keep `QCSDKManager.shareInstance.delegate` set to active controller
- For robust UX, show indicators during Wi-Fi discovery or long download steps
