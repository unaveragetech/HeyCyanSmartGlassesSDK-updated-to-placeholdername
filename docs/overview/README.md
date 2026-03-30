# HeyCyan Smart Glasses SDK Overview

## Product
HeyCyan Smart Glasses are wearable AR/AI-enabled smart glasses with camera, microphone, speaker, wireless connectivity, and AI cloud/edge capabilities. This SDK provides Bluetooth BLE-based device control and status reporting, plus Wi-Fi transfer and P2P media download pathways.

## Supported capabilities
- Pairing + BLE connectivity (scan/connect/reconnect/unbind)
- Device control modes: photo capture, video record, audio record, transfer mode, OTA mode, AI-photo, translation, speech recognition, find device, restart, factory reset
- Device status retrieval: battery, media counts (photo/video/audio), version info, MAC address
- AI features: AI image callback (`didReceiveAIChatImageData`), AI speaking mode control, translation mode toggles
- Streaming and recording: audio and video start/stop, keyboard short commands
- Firmware / DFU: enter DFU, firmware init, packet upload, check status, finish, band switch, OTA URL
- Wi-Fi transfer (hotspot mode), Wi-Fi IP detection, manual iOS NEHotspot join, network scanning for media server
- Media management: list media, delete single/all, download thumbnails, local gallery viewer
- Additional features: volume control, BT status, wearing detection, voice wakeup, device configuration

## SDK components
- iOS: `ios/QCSDK.framework` with `QCSDKManager`, `QCSDKCmdCreator`, `OdmBleConstants`, `QCDFU_Utils`, `QCVersionHelper`, `QCVolumeInfoModel`
- iOS demo: `ios/QCSDKDemo` (UI flows, media download, WiFi handler, scan/connect manager)
- Android sample: `android/GlassesSDKSample` (BLE manager, `LargeDataHandler`, P2P file download, stateful UI), includes translation and AI event parsing

## Recommended dev route summary
1. Implement generic BLE manager for discovery + connection via `QCCentralManager` (iOS) and `BleOperateManager` (Android)
2. Build device mode engine using `QCSDKCmdCreator` mode commands and `LargeDataHandler.glassesControl` APDU bytes
3. Connect media request paths: photos/videos/audio states, `getDeviceMedia`, `getDeviceWifiIP`
4. Implement Wi-Fi transfer process (`openWifiWithMode(Transfer)` + connect AP + download `/files/media.config` + file loop)
5. Add UI for mode toggles, device status, media gallery, and progress indicators
6. Add DFU/OTA workflow as separate advanced section to support firmware updates

