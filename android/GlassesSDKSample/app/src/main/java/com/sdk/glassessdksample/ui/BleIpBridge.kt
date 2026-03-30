package com.sdk.glassessdksample.ui

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

class BleIpBridge {
    private val _ip = MutableStateFlow<String?>(null)
    val ip = _ip.asStateFlow()

    fun onCharacteristicChanged(value: ByteArray) {
        // Defensive parse: accept utf8 text and typical 'ip:' prefix.
        val msg = try {
            value.toString(StandardCharsets.UTF_8).trim()
        } catch (e: Exception) {
            Log.e("BleIpBridge", "Failed to decode BLE bytes", e)
            return
        }

        Log.i("BleIpBridge", "Received BLE message: $msg")

        // Find IPv4 or IPv6; currently we expect v4 from device.
        val ipv4Regex = Regex("""(\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b)""")
        val ipCandidate = ipv4Regex.find(msg)?.value

        if (!ipCandidate.isNullOrEmpty()) {
            Log.i("BleIpBridge", "Parsed device IP: $ipCandidate")
            _ip.value = ipCandidate
        } else {
            Log.w("BleIpBridge", "No IP address found in BLE payload")
        }
    }
}