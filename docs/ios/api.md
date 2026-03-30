# iOS SDK API Reference

This document summarizes the public API in `ios/QCSDK.framework`.

## QCSDKManager

**Singleton**
- `+ (instancetype)shareInstance` - gets shared manager.

**Properties**
- `BOOL debug` - toggle debugging logs.
- `id<QCSDKManagerDelegate> delegate` - delegate for updates.

**Peripheral management**
- `- (void)addPeripheral:(CBPeripheral *)peripheral finished:(void (^)(BOOL))finished` - add connected peripheral to internal list.
- `- (void)removePeripheral:(CBPeripheral *)peripheral` - remove a peripheral.
- `- (void)removeAllPeripheral` - disconnect and cleanup all peripherals.

### Delegate callbacks (`QCSDKManagerDelegate`)
- `- (void)didUpdateBatteryLevel:(NSInteger)battery charging:(BOOL)charging`
- `- (void)didUpdateMediaWithPhotoCount:(NSInteger)photo videoCount:(NSInteger)video audioCount:(NSInteger)audio type:(NSInteger)type`
- `- (void)didUpdateWiFiUpgradeProgressWithDownload:(NSInteger)download upgrade1:(NSInteger)upgrade1 upgrade2:(NSInteger)upgrade2`
- `- (void)didReceiveWiFiUpgradeResult:(BOOL)success`
- `- (void)didReceiveAIChatImageData:(NSData *)imageData` (AI image stream data)

## QCSDKCmdCreator

Utility wrappers for BLE commands and device operations.

### Mode controls
- `+ setDeviceMode:success:fail:`
- `+ openWifiWithMode:success:fail:`
- `+ setVideoInfo:duration:success:fail:`
- `+ getVideoInfoSuccess:fail:`
- `+ setAudioInfo:duration:success:fail:`
- `+ getAudioInfoSuccess:fail:`

### Status and info
- `+ getDeviceWifiIPSuccess:failed:`
- `+ getDeviceMedia:fail:`
- `+ getDeviceBattery:fail:`
- `+ getDeviceVersionInfoSuccess:fail:`
- `+ getDeviceMacAddressSuccess:fail:`
- `+ isPeripheralFreeNow` (BOOL)

### Media management
- `+ deleleteAllMediasSuccess:fail:`
- `+ deleleteMedia:success:fail:`
- `+ getThumbnail:success:fail:`

### System utilities
- `+ setupDeviceDateTime:`
- `+ sendOTAFileLink:finished:`

### Voice/AI features
- `+ sendVoiceHeartbeatWithFinished:`
- `+ getVoiceWakeupWithFinished:`
- `+ setVoiceWakeup:finished:`
- `+ getWearingDetectionWithFinished:`
- `+ setWearingDetection:finished:`
- `+ getDeviceConfigWithFinished:`
- `+ setAISpeekModel:finished:`
- `+ getVolumeWithFinished:`
- `+ setVolume:finished:`
- `+ setBTStatus:finished:`
- `+ getBTStatusWithFinished:`

## DFU / Firmware update
- `+ switchToDFU:`
- `+ initDFUFirmwareType:binFileSize:checkSum:crc16:finished:`
- `+ sendFilePacketData:serialNumber:finished:`
- `+ checkMyFirmwareWithData:finished:`
- `+ finishDFU:`
- `+ checkCurrentStatusWithData:finished:`
- `+ getDFUBandTypeInfoSuccess:fail:`
- `+ switchToOneBandDFU:`

## QCDFU_Utils constants and enums

- `ODM_DFU_UUID_Service`, `ODM_DFU_UUID_WriteCharacteristic`, and `ODM_DFU_UUID_NotifyCharacteristic`
- `ODM_DFU_Operation` command op codes (StartDfuRequest, InitializeDfuParametersRequest, etc.)
- `QCOperatorDeviceMode` with modes:
  - `QCOperatorDeviceModePhoto`, `QCOperatorDeviceModeVideo`, `QCOperatorDeviceModeTransfer`, `QCOperatorDeviceModeOTA`, `QCOperatorDeviceModeAIPhoto`, `QCOperatorDeviceModeSpeechRecognition`, `QCOperatorDeviceModeAudio`, `QCOperatorDeviceModeTransferStop`, `QCOperatorDeviceModeFactoryReset`, `QCOperatorDeviceModeSpeechRecognitionStop`, `QCOperatorDeviceModeAudioStop`, `QCOperatorDeviceModeFindDevice`, `QCOperatorDeviceModeRestart`, `QCOperatorDeviceModeNoPowerP2P`, `QCOperatorDeviceModeSpeakStart`, `QCOperatorDeviceModeSpeakStop`, `QCOperatorDeviceModeTranslateStart`, `QCOperatorDeviceModeTranslateStop`
- `QGAISpeakMode` (Start/Hold/Stop/Thinking/NoNet)
- DFU helper enums: `ODM_DFU_Device_Process_Status`, `ODM_DFU_FirmwareType`, `ODM_DFU_BandType`, `ODM_DFU_Error_Code`, etc.

## OdmBleConstants

Notification names (KVO) and BLE states:
- `OdmNotifyPedometer`, `OdmWphPeripheral`, `OdmNotifyRetrieveFinish`, `OdmNotifyD2P`, etc.
- `BLECONNECTSTATE`: `BLECONNECTSTATEOFF`, `BLECONNECTSTATEON`, `BLECONNECTSTATEFAIL`

## QCVersionHelper & QCVolumeInfoModel
- `+ frameworkVersion` (version query function)
- `QCVolumeInfoModel` fields (music/call/system volume ranges and current levels)

## Notes
- All asynchronous calls take success/fail handlers; handle background thread safety.
- Use `setDeviceMode` and `isPeripheralFreeNow` to avoid command conflicts (e.g., setting mode while device busy).
- In iOS, `openWifiWithMode` + `getDeviceWifiIP` is required for media transfer.
