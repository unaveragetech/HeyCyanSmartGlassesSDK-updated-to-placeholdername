package com.sdk.glassessdksample.feature

import android.util.Log
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.bluetooth.DeviceManager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ConnectionManager {
    private const val TAG = "ConnectionManager"
    private const val MAX_RECONNECT_ATTEMPTS = 5
    private const val BASE_RECONNECT_DELAY_MS = 1500L
    private const val STATUS_CHECK_DELAY_MS = 4000L

    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0

    val isConnected: Boolean
        get() = BleOperateManager.getInstance().isConnected

    val deviceAddress: String?
        get() = DeviceManager.getInstance().deviceAddress

    fun connect(onResult: (success: Boolean) -> Unit) {
        val address = deviceAddress
        if (address.isNullOrEmpty()) {
            Log.w(TAG, "No bound device address")
            onResult(false)
            return
        }

        try {
            BleOperateManager.getInstance().connectDirectly(address)
            onResult(true)
        } catch (t: Throwable) {
            Log.e(TAG, "connect error", t)
            onResult(false)
        }
    }

    fun connectWithRetry(onResult: (success: Boolean, message: String) -> Unit) {
        reconnectJob?.cancel()
        reconnectAttempt = 0
        attemptReconnect(onResult)
    }

    private fun attemptReconnect(onResult: (success: Boolean, message: String) -> Unit) {
        val address = deviceAddress
        if (address.isNullOrEmpty()) {
            Log.w(TAG, "No bound device for reconnect")
            onResult(false, "No bound device")
            return
        }

        if (isConnected) {
            onResult(true, "Already connected")
            return
        }

        reconnectAttempt++
        Log.i(TAG, "Reconnect attempt #$reconnectAttempt")

        try {
            BleOperateManager.getInstance().connectDirectly(address)
        } catch (t: Throwable) {
            Log.e(TAG, "Reconnect connectDirectly error", t)
        }

        reconnectJob = CoroutineScope(Dispatchers.Default).launch {
            delay(STATUS_CHECK_DELAY_MS)
            if (isConnected) {
                withContext(Dispatchers.Main) {
                    onResult(true, "Connected after attempt $reconnectAttempt")
                }
            } else {
                if (reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Failed after $reconnectAttempt attempts")
                    }
                } else {
                    val backoff = (BASE_RECONNECT_DELAY_MS * (1 shl (reconnectAttempt - 1))).coerceAtMost(30000L)
                    Log.i(TAG, "Retrying in ${backoff}ms")
                    delay(backoff)
                    attemptReconnect(onResult)
                }
            }
        }
    }

    fun cancelAutoReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempt = 0
    }

    fun disconnect() {
        cancelAutoReconnect()
        try {
            BleOperateManager.getInstance().unBindDevice()
        } catch (t: Throwable) {
            Log.e(TAG, "disconnect error", t)
        }
    }
}
