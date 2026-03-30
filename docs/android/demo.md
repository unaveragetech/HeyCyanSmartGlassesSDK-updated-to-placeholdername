# Android Demo App Walkthrough

This document explains the Android sample app flow: `android/GlassesSDKSample/app/src/main/java/com/sdk/glassessdksample/MainActivity.kt`.

## UI actions and mapped APIs
- `btnScan`: starts device discovery via `requestLocationPermission -> DeviceBindActivity`.
- `btnConnect`: connect to glasses with `BleOperateManager.getInstance().connectDirectly(DeviceManager.getInstance().deviceAddress)`.
- `btnDisconnect`: `BleOperateManager.getInstance().unBindDevice()`.
- `btnAddListener`: subscribe to device event callbacks via `LargeDataHandler.getInstance().addOutDeviceListener(...)`.
- `btnSetTime`: `LargeDataHandler.getInstance().syncTime`.
- `btnVersion`: `LargeDataHandler.getInstance().syncDeviceInfo` to get firmware/hardware versions.
- `btnCamera`: `LargeDataHandler.getInstance().glassesControl(byteArrayOf(0x02,0x01,0x01))`.
- `btnVideo`: `glassesControl` with `0x02` or `0x03` for start/stop.
- `btnRecord`: `glassesControl` with `0x08` or `0x0c` for start/stop audio.
- `btnThumbnail`: request local image thumbnail and parse callback response.
- `btnBt`: start classic BT scan via `BleOperateManager.classicBluetoothStartScan()`.
- `btnBattery`: battery callback and `syncBattery`.
- `btnVolume`: `getVolumeControl` and read volume info fields.
- `btnMediaCount`: `glassesControl(byteArrayOf(0x02,0x04))` to query pending media counts.
- `btnDataDownload`: advanced BLE+WiFi-P2P pipeline.

## Data download flow
1. Ensure BLE connected and permissions granted (`NEARBY_WIFI_DEVICES`).
2. Fetch device IP from BLE (`getDeviceIpFromBLE`, placeholder, likely via `glassesControl` throwback)
3. Create P2P group with `WifiP2pManagerSingleton`.
4. Test connectivity to device IP via HTTP (`/files/media.config`).
5. Download media list (manifest) and parse JPEG file names.
6. Download each JPG (`/files/<file>`) and save to local app folder.
7. Convert to album entry (TODO to implement).

## P2P and networking helper classes
- `WifiP2pManagerSingleton`: wraps Wi-Fi P2P lifecycle and callbacks including `onWifiP2pEnabled`, `onConnected`, `onDisconnected`, etc.
- `P2PController` and `BleIpBridge` help map BLE operation and IP access to local P2P network.

## Event parsing in `MyDeviceNotifyListener`
- Index `loadData[6]` indicates event type, see comment cases for:
  - `0x05` battery report
  - `0x02` quick AI image recognizer and thumbnails
  - `0x03` mic speak status
  - `0x04` OTA progress
  - `0x0c` speech pause event or AI prompt
  - `0x0d` unbind event
  - `0x0e` memory low event
  - `0x10` translation pause event
  - `0x12` volume change event

## Android OS guidance
- Requires Bluetooth and location or NEARBY_WIFI_DEVICES permissions.
- Use `XXPermissions` library for runtime permissions convenience.
- Handle Android 13 restrictions by request the newest permission types.
- Keep `EventBus` lifecycle in mind (used in sample) to avoid leaks.
- Use background thread/coroutines for network downloads while keeping UI responsive.

## Notes
- The Android sample is functional but includes placeholder IP and incomplete local album integration.
- Add robust error transforms for network error codes and fallback IPs similarly to iOS.
- Consider encapsulating BLE command bytes into an API wrapper class to avoid hardcoded byte arrays.
