# New App Architecture Recommendation (HeyCyan Smart Glasses)

This section outlines the best route for building a new app on top of the existing HeyCyan SDK.

## 1. Goals
- Reliable BLE pairing and reconnection.
- Mode management (photo/video/audio/AI/translation/transfer).
- Media fetch + display (local and remote transfer).
- DFU updates and OTA.
- Clean UX with status and error handling.
- Cross-platform consistency (iOS/Android parity where possible).

## 2. Core layers

### 2.1 `ConnectionManager`
- Wraps `QCCentralManager` (iOS) and `BleOperateManager` (Android).
- Exposes methods: `scan()`, `connect(device)`, `disconnect()`, `isConnected`, `onStateChange`.
- Maintains current device metadata (name, UUID, MAC).
- Implements auto-reconnect if disconnected unexpectedly.

### 2.2 `DeviceCommandService`
- Uses `QCSDKCmdCreator` iOS + `LargeDataHandler.glassesControl` Android.
- Provides high-level methods:
  - `setModeCapture()`, `setModeVideo(start/stop)`, `setModeAudio(start/stop)`, `setModeAIPhoto()`, `setModeTranslate(start/stop)`
  - `getBattery()`, `getMediaInfo()`, `getVersionInfo()`, `getMacAddress()`
  - `setDeviceTime()`
  - `setVoiceWakeup(on)`, `setWearingDetection(on)`
  - `setVolume`, `getVolume`
  - `sendOTAUrl`, `enterDFUMode`, `uploadFirmware` (DFU steps)
  - `isPeripheralFreeNow()` and `busy state` guarding

### 2.3 `WifiTransferService`
- orchestrates `openWifiWithMode(Transfer)` and `getDeviceWifiIP()` (or BLE IP path)
- iOS: NEHotspotConfiguration + system network status check + multiple fallback IPs
- Android: Wi-Fi P2P group creation + group ownership + local HTTP loops
- File format: `/files/media.config`, `/files/<filename>`
- Download progress + retry and cancellation support.

### 2.4 `MediaManager`
- parses media counts and listings
- local storage path (`Documents/GlassesMedia` iOS, app-specific folder Android)
- thumbnail generation, gallery UI, delete file functions
- upload media to cloud or share with system.

### 2.5 `AiAndTranslateService`
- `didReceiveAIChatImageData` bridging to UI for preview
- command toggles for `ai speak` and `translation` (QE operator modes)
- handle text or voice translation, maybe combine with local speech-to-text on phone

## 3. UX patterns
- Always show connection state, battery, and mode status near top.
- Warnings when switching modes if device busy (via `isPeripheralFreeNow` or similar busy indicators).
- For transfer flow, show explicit steps: `Turning on transfer mode -> Connect to WiFi -> Finding device IP -> Download manifest -> Download files`.
- For Permissions, preflight request with explanation.
- For DFU, show full progress, success, and fallback on failure.

## 4. Translation + AI strategy
- Start translation with `setDeviceMode(TranslateStart)` and stop with `TranslateStop`.
- For AI conversation and image generation, use modes from `QCOperatorDeviceMode` and process callbacks from `didReceiveAIChatImageData`.
- Build standard server middleware to send/receive text as needed (optional for improved language switching).

## 5. Testing & validation
- Unit test command sequences for device state transition.
- Integration tests against an actual glasses device or a mocked BLE endpoint.
- Ensure rich logging + error observer on all asynchronous results.
- Stress test reconnect and Wi-Fi failover (multiple times).

## 6. Security and privacy
- Request minimal runtime permissions.
- Never persist sensitive tokens in plain text.
- Handle camera/microphone access as per iOS/Android specs.
- Use cleartext traffic exceptions only for local device endpoints, not Internet.

## 7. Directory for docs and contribution
- Keep this `docs/` folder updated with feature APIs and sample flows.
- Add a `docs/Roadmap.md` later for feature prioritization.

## 8. Next implementation milestones
1. Implement `ConnectionManager` -> `DeviceCommandService` basics.
2. Build `WiFiTransferService` as robust cross-platform module.
3. Add high-level `MediaManager` and UI also gallery.
4. Integrate AI image flow and translation toggles.
5. Add DFU/OTA path and verification.
