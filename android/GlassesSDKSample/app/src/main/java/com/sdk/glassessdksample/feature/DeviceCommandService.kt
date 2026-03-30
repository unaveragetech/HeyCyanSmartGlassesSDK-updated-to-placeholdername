package com.sdk.glassessdksample.feature

import android.util.Log
import com.oudmon.ble.base.communication.LargeDataHandler
import com.oudmon.ble.base.communication.bigData.resp.GlassesDeviceNotifyRsp

object DeviceCommandService {
    private const val TAG = "DeviceCommandService"

    fun takePhoto(callback: (success: Boolean, message: String) -> Unit) {
        sendControl(byteArrayOf(0x02, 0x01, 0x01), callback)
    }

    fun startVideo(callback: (Boolean, String) -> Unit) {
        sendControl(byteArrayOf(0x02, 0x01, 0x02), callback)
    }

    fun stopVideo(callback: (Boolean, String) -> Unit) {
        sendControl(byteArrayOf(0x02, 0x01, 0x03), callback)
    }

    fun startAudio(callback: (Boolean, String) -> Unit) {
        sendControl(byteArrayOf(0x02, 0x01, 0x08), callback)
    }

    fun stopAudio(callback: (Boolean, String) -> Unit) {
        sendControl(byteArrayOf(0x02, 0x01, 0x0c), callback)
    }

    fun queryMediaCount(callback: (Boolean, String) -> Unit) {
        sendControl(byteArrayOf(0x02, 0x04), callback)
    }

    fun requestDeviceInfo(callback: (Boolean, String) -> Unit) {
        LargeDataHandler.getInstance().syncDeviceInfo { _, response ->
            if (response != null) {
                val text = "hw:${response.hardwareVersion} fw:${response.firmwareVersion} wifiHW:${response.wifiHardwareVersion} wifiFW:${response.wifiFirmwareVersion}"
                callback(true, text)
            } else {
                callback(false, "no response")
            }
        }
    }

    fun setTime(callback: (Boolean, String) -> Unit) {
        LargeDataHandler.getInstance().syncTime { _, _ -> callback(true, "time set") }
    }

    fun syncBattery(callback: (Boolean, String) -> Unit) {
        try {
            LargeDataHandler.getInstance().addBatteryCallBack("demo_battery") { _, response ->
                if (response != null) {
                    val level = response.battery
                    val charging = response.isCharging()
                    callback(true, "Battery: $level%, charging=$charging")
                } else {
                    callback(false, "No battery response")
                }
            }
            LargeDataHandler.getInstance().syncBattery()
        } catch (t: Throwable) {
            callback(false, "syncBattery failed: ${t.message}")
        }
    }

    fun removeBatteryListener() {
        try {
            LargeDataHandler.getInstance().removeBatteryCallBack("demo_battery")
        } catch (ignored: Throwable) {
        }
    }

    fun getVolumeInfo(callback: (Boolean, String) -> Unit) {
        try {
            LargeDataHandler.getInstance().getVolumeControl { _, response ->
                if (response != null) {
                    val info = "Music [${response.currVolumeMusic}/${response.maxVolumeMusic}] " +
                            "System [${response.currVolumeSystem}/${response.maxVolumeSystem}] " +
                            "Call [${response.currVolumeCall}/${response.maxVolumeCall}]";
                    callback(true, info)
                } else {
                    callback(false, "No volume response")
                }
            }
        } catch (t: Throwable) {
            callback(false, "getVolumeControl failed: ${t.message}")
        }
    }

    fun enableClassicBtScan(callback: (Boolean, String) -> Unit) {
        try {
            LargeDataHandler.getInstance().syncClassicBluetooth { _, resp ->
                callback(true, "Classic BT sync response: $resp")
            }
        } catch (t: Throwable) {
            callback(false, "syncClassicBluetooth failed: ${t.message}")
        }
    }

    private fun sendControl(cmd: ByteArray, callback: (Boolean, String) -> Unit) {
        try {
            LargeDataHandler.getInstance().glassesControl(cmd) { _, result ->
                try {
                    if (result != null && result.errorCode == 0) {
                        callback(true, "ok type=${result.workTypeIng}")
                    } else {
                        callback(false, "fail dataType=${result?.dataType} error=${result?.errorCode}")
                    }
                } catch (e: Exception) {
                    callback(false, "result parse exception")
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "control send failed", t)
            callback(false, t.localizedMessage ?: "error")
        }
    }
}
