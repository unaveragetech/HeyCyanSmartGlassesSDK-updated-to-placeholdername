# Android SDK API Reference

This document summarizes Android-side APIs used in `android/GlassesSDKSample`.

## Core SDK classes

### BleOperateManager (BLE control)
- `.getInstance()` singleton
- `.connectDirectly(deviceAddress)` to connect
- `.unBindDevice()` to disconnect
- `.classicBluetoothStartScan()` for Bluetooth scanning
- `.isConnected` status property

### DeviceManager
- `.getInstance().deviceAddress` to obtain bound device MAC

### LargeDataHandler (high-level command manager)
- `.addOutDeviceListener(code, listener)` to subscribe notifications
- `.syncTime(callback)` set device date/time
- `.syncDeviceInfo(callback)` fetch versions and other status fields: `firmwareVersion`, `hardwareVersion`, `wifiFirmwareVersion`, `wifiHardwareVersion`
- `.glassesControl(cmd, callback)` send low-level control data bytes (mode switching, capture commands)
- `.addBatteryCallBack(id, callback)` observe battery events
- `.syncBattery()` query battery level
- `.getVolumeControl(callback)` volume state

### GlassesDeviceNotifyListener
- Override `parseData(cmdType, response)`
- Use `response.loadData` to interpret device event codes (battery, AI image, OTA progress, translation status, volume, etc.)

### P2P / Wifi
- `WifiP2pManagerSingleton` helper for group creation and peer connections
- `P2PController`, `BleIpBridge` for converting BLE-to-IP and managing hotspot transports

## Command bytes mapping (extract from sample)
- `byteArrayOf(0x02, 0x01, 0x01)` : camera/photo mode
- `byteArrayOf(0x02, 0x01, 0x02/0x03)` : video start/stop
- `byteArrayOf(0x02, 0x01, 0x08/0x0c)` : audio record start/stop
- `byteArrayOf(0x02, 0x01, 0x06, thumbnailSize, thumbnailSize, 0x02)` : thumbnail retrieval
- `byteArrayOf(0x02, 0x04)` : query media count

## Event response types
- Media status: `dataType == 4`, includes `imageCount`, `videoCount`, `recordCount`
- OTA progress: `loadData[6]` == 0x04 with fields 7/8/9
- Volume updates: `loadData[6] == 0x12`, values in indices 8..19

## Notes
- Use `syncDeviceInfo` and `syncBattery` for periodic device state refresh.
- `LargeDataHandler` callbacks should be mapped to main/UI thread as needed.
- Request runtime permissions for location / nearby Wi-Fi devices and Bluetooth on Android 12+.
- `Intent` for enabling Bluetooth.
- App network code uses plain HTTP to device local IP; ensure `networkSecurityConfig` allows cleartext for local addresses.
