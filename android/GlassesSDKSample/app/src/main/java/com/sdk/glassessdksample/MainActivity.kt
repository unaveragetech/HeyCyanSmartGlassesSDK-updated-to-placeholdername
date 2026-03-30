package com.sdk.glassessdksample

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.core.app.ActivityCompat
import com.google.android.material.tabs.TabLayoutMediator
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.bluetooth.DeviceManager
import com.oudmon.ble.base.communication.LargeDataHandler
import com.oudmon.ble.base.communication.bigData.resp.GlassesDeviceNotifyListener
import com.oudmon.ble.base.communication.bigData.resp.GlassesDeviceNotifyRsp
import com.sdk.glassessdksample.databinding.AcitivytMainBinding
import com.sdk.glassessdksample.ui.BluetoothUtils
import com.sdk.glassessdksample.ui.DeviceBindActivity
import com.sdk.glassessdksample.ui.MainPagerAdapter
import com.sdk.glassessdksample.ui.hasBluetooth
import com.sdk.glassessdksample.ui.requestAllPermission
import com.sdk.glassessdksample.ui.requestBluetoothPermission
import com.sdk.glassessdksample.ui.requestLocationPermission
import com.sdk.glassessdksample.ui.requestNearbyWifiDevicesPermission
import com.sdk.glassessdksample.ui.setOnClickListener
import com.sdk.glassessdksample.ui.startKtxActivity
import com.sdk.glassessdksample.feature.ConnectionManager
import com.sdk.glassessdksample.feature.CustomFeatureApi
import com.sdk.glassessdksample.feature.DeviceCommandService
import com.sdk.glassessdksample.feature.TinyLLMService
import com.sdk.glassessdksample.feature.VoiceCommandService
import com.sdk.glassessdksample.ui.P2PController
import com.sdk.glassessdksample.ui.wifi.p2p.WifiP2pManagerSingleton
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import org.greenrobot.eventbus.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {
    private lateinit var binding: AcitivytMainBinding
    private val deviceNotifyListener by lazy { MyDeviceNotifyListener() }

    private val bleIpBridge = com.sdk.glassessdksample.ui.BleIpBridge()
    private var voiceCommandService: VoiceCommandService? = null
    private val PREFS_NAME = "glasses_state"
    private val PREF_KEY_DEVICE_IP = "device_ip"
    private lateinit var sharedPreferences: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto theme by local hour
        setThemeByTime()

        binding = AcitivytMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mark this build clearly for verification
        binding.toolbar.title = "HeyCyan Glasses v2026"
        Toast.makeText(this, "Running updated demo build (v2026)", Toast.LENGTH_LONG).show()

        // Splash / loading screen setup
        binding.loadingContainer.visibility = View.VISIBLE
        binding.mainContent.visibility = View.GONE

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // Observe IP from BLE bridge and persist for WiFi data download
        lifecycleScope.launchWhenStarted {
            bleIpBridge.ip.collect { ip ->
                if (!ip.isNullOrBlank()) {
                    Log.i("MainActivity", "BLE reported glasses IP: $ip")
                    saveDeviceIpCache(ip)
                }
            }
        }

        if (!sharedPreferences.getBoolean("terms_accepted", false)) {
            showFirstRunDisclaimer()
        } else {
            beginAppFlow()
        }
    }

    private fun showFirstRunDisclaimer() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Prototype Disclaimer")
            .setMessage("This is a prototype. We provide no liability for any results. By continuing, you agree to use experimental features at your own risk. This app is for testing and demonstration.")
            .setCancelable(false)
            .setPositiveButton("Accept") { _, _ ->
                sharedPreferences.edit().putBoolean("terms_accepted", true).apply()
                beginAppFlow()
            }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .create()
        dialog.show()
    }

    private fun beginAppFlow() {
        binding.loadingContainer.visibility = View.VISIBLE
        binding.mainContent.visibility = View.GONE

        binding.mainContent.postDelayed({
            binding.loadingContainer.visibility = View.GONE
            binding.mainContent.visibility = View.VISIBLE
            showWelcomeTour()
        }, 1200)
    }

    private fun showWelcomeTour() {
        val didTour = sharedPreferences.getBoolean("tour_completed", false)
        if (didTour) {
            setupMainTabs(); return
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Welcome to the Glasses Assistant")
            .setMessage("This app helps you connect to your glasses, run smart workflows, and customize your assistant. If Bluetooth pairing fails, you can continue in offline mode and use local features.")
            .setCancelable(false)
            .setPositiveButton("Continue") { _, _ ->
                sharedPreferences.edit().putBoolean("tour_completed", true).apply()
                setupMainTabs()
            }
            .create()
        dialog.show()
    }
    inner class PermissionCallback : OnPermissionCallback {
        override fun onGranted(permissions: MutableList<String>, all: Boolean) {
            if (!all) {

            }else{
                startKtxActivity<DeviceBindActivity>()
            }
        }

        override fun onDenied(permissions: MutableList<String>, never: Boolean) {
            super.onDenied(permissions, never)
            if(never){
                XXPermissions.startPermissionActivity(this@MainActivity, permissions);
            }
        }

    }


    override fun onResume() {
        super.onResume()
        try {
            if (!BluetoothUtils.isEnabledBluetooth(this)) {
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                }
                startActivityForResult(intent, 300)
            }
        } catch (e: Exception) {
        }
        if (!hasBluetooth(this)) {
            requestBluetoothPermission(this, BluetoothPermissionCallback())
        }

        requestAllPermission(this, OnPermissionCallback { permissions, all ->  })
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceCommandService?.release()
    }

    inner class BluetoothPermissionCallback : OnPermissionCallback {
        override fun onGranted(permissions: MutableList<String>, all: Boolean) {
            if (!all) {

            }
        }

        override fun onDenied(permissions: MutableList<String>, never: Boolean) {
            super.onDenied(permissions, never)
            if (never) {
                XXPermissions.startPermissionActivity(this@MainActivity, permissions)
            }
        }

    }

    private fun setupMainTabs() {
        binding.viewPager.adapter = MainPagerAdapter(this)
        com.google.android.material.tabs.TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Devices"
                1 -> "Controls"
                else -> "Tab ${position + 1}"
            }
        }.attach()
    }

    fun startDataDownload() {
        Log.i("DataDownload", "Starting BLE+WiFi P2P data download...")
        
        // 检查蓝牙连接状态
        if (!BleOperateManager.getInstance().isConnected) {
            Log.e("DataDownload", "Bluetooth not connected. Please connect to glasses first.")
            return
        }
        
        // 检查必要的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!XXPermissions.isGranted(this, "android.permission.NEARBY_WIFI_DEVICES")) {
                Log.e("DataDownload", "NEARBY_WIFI_DEVICES permission not granted")
                return
            }
        }
        
        // 启动P2P连接和数据下载
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. 通过BLE获取眼镜的IP地址
                val deviceIp = getDeviceIpFromBLE()
                if (deviceIp.isNullOrEmpty()) {
                    Log.e("DataDownload", "Failed to get device IP from BLE")
                    return@launch
                }
                
                Log.i("DataDownload", "Device IP from BLE: $deviceIp")
                
                // 2. 建立WiFi P2P连接 - 使用新的WifiP2pManagerSingleton
                val wifiP2pManager = WifiP2pManagerSingleton.getInstance(this@MainActivity)
                val receiver = wifiP2pManager.registerReceiver()
                
                try {
                    // 添加回调监听器
                    wifiP2pManager.addCallback(object : WifiP2pManagerSingleton.WifiP2pCallback {
                        override fun onWifiP2pEnabled() {
                            Log.i("DataDownload", "WiFi P2P enabled, creating P2P group...")
                            // 创建P2P组（手机作为GO）
                            wifiP2pManager.createGroup { success ->
                                if (success) {
                                    Log.i("DataDownload", "P2P group created successfully")
                                    // 等待P2P连接完全建立
                                    CoroutineScope(Dispatchers.IO).launch {
                                        delay(2000) // 等待2秒让连接稳定
                                        
                                        // 测试连接是否可用
                                        if (testConnection(deviceIp)) {
                                            Log.i("DataDownload", "Connection test successful, starting downloads...")
                                            
                                            // 3. 下载媒体文件列表
                                            downloadMediaList(deviceIp)
                                        } else {
                                            Log.e("DataDownload", "Connection test failed, cannot reach device")
                                            runOnUiThread {
                                                showDownloadError("Cannot connect to glasses device. Please check P2P connection.")
                                            }
                                        }
                                    }
                                } else {
                                    Log.e("DataDownload", "Failed to create P2P group")
                                    runOnUiThread {
                                        showDownloadError("Failed to create P2P group")
                                    }
                                }
                            }
                        }
                        
                        override fun onWifiP2pDisabled() {
                            Log.e("DataDownload", "WiFi P2P disabled")
                        }
                        
                        override fun onPeersChanged(peers: Collection<WifiP2pDevice>) {
                            Log.i("DataDownload", "Found ${peers.size} P2P devices")
                        }
                        
                        override fun onThisDeviceChanged(device: WifiP2pDevice) {
                            Log.i("DataDownload", "This device changed: ${device.deviceName} - ${device.status}")
                        }
                        
                        override fun onConnected(info: WifiP2pInfo) {
                            Log.i("DataDownload", "P2P connected: groupFormed=${info.groupFormed}, isGroupOwner=${info.isGroupOwner}")
                        }
                        
                        override fun onDisconnected() {
                            Log.i("DataDownload", "P2P disconnected")
                        }
                        
                        override fun onPeerDiscoveryStarted() {
                            Log.i("DataDownload", "Peer discovery started")
                        }
                        
                        override fun onPeerDiscoveryFailed(reason: Int) {
                            Log.e("DataDownload", "Peer discovery failed: $reason")
                        }
                        
                        override fun onConnectRequestSent() {
                            Log.i("DataDownload", "Connect request sent")
                        }
                        
                        override fun onConnectRequestFailed(reason: Int) {
                            Log.e("DataDownload", "Connect request failed: $reason")
                        }
                        
                        override fun connecting() {
                            Log.i("DataDownload", "Connecting to P2P device...")
                        }
                        
                        override fun cancelConnect() {
                            Log.i("DataDownload", "P2P connection cancelled")
                        }
                        
                        override fun cancelConnectFail(reason: Int) {
                            Log.e("DataDownload", "Cancel connect failed: $reason")
                        }
                        
                        override fun retryAlsoFailed() {
                            Log.e("DataDownload", "P2P connection retry failed")
                        }
                    })
                    
                } finally {
                    // 清理P2P连接
                    wifiP2pManager.removeGroup { success ->
                        Log.i("DataDownload", "P2P group removed: $success")
                    }
                    wifiP2pManager.unregisterReceiver(receiver)
                }
                
            } catch (e: Exception) {
                Log.e("DataDownload", "Error during data download: ${e.message}", e)
            }
        }
    }
    
    private fun getDeviceIpFromBLE(): String? {
        val bleIp = bleIpBridge.ip.value
        if (!bleIp.isNullOrBlank()) {
            saveDeviceIpCache(bleIp)
            return bleIp
        }

        val cachedIp = getDeviceIpCache()
        if (!cachedIp.isNullOrBlank()) {
            Log.i("MainActivity", "Using cached glass IP: $cachedIp")
            return cachedIp
        }

        // Fall back to device manager or fixed default only if absolutely necessary
        val fromDeviceManager = DeviceManager.getInstance().deviceAddress?.takeIf { it.isNotBlank() }
        if (!fromDeviceManager.isNullOrBlank()) {
            // Not an IP, but keep for fallback in case user wants to identify device
            Log.i("MainActivity", "No BLE IP; using bound device address as fallback: $fromDeviceManager")
        }

        return null
    }

    private fun saveDeviceIpCache(ip: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(PREF_KEY_DEVICE_IP, ip).apply()
    }

    private fun getDeviceIpCache(): String? {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(PREF_KEY_DEVICE_IP, null)
    }

    private fun downloadMediaList(deviceIp: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "http://$deviceIp/files/media.config"
                Log.i("DataDownload", "Downloading media list from: $url")
                
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 30000
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val content = inputStream.bufferedReader().use { it.readText() }
                    
                    // 显示下载的内容
                    Log.i("DataDownload", "=== MEDIA CONFIG CONTENT ===")
                    Log.i("DataDownload", content)
                    Log.i("DataDownload", "=== END MEDIA CONFIG ===")
                    
                    // 解析媒体文件列表
                    parseMediaList(content)
                    
                    withContext(Dispatchers.Main) {
                        showDownloadSuccess("Media list downloaded successfully")
                    }
                } else {
                    Log.e("DataDownload", "Failed to download media list. Response code: ${connection.responseCode}")
                    withContext(Dispatchers.Main) {
                        showDownloadError("Failed to download media list. Response code: ${connection.responseCode}")
                    }
                }
                
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("DataDownload", "Error downloading media list: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    when (e) {
                        is java.io.IOException -> {
                            if (e.message?.contains("Cleartext HTTP traffic") == true) {
                                showDownloadError("Network security blocked HTTP connection. Please check app settings.")
                            } else if (e.message?.contains("Failed to connect") == true) {
                                showDownloadError("Cannot connect to glasses device. Please ensure P2P connection is established.")
                            } else {
                                showDownloadError("Network error: ${e.message}")
                            }
                        }
                        else -> showDownloadError("Download failed: ${e.message}")
                    }
                }
            }
        }
    }
    
    private fun parseMediaList(content: String) {
        // 解析媒体配置文件内容 - 这是一个包含JPG文件名的文本文件
        Log.i("DataDownload", "Parsing media list content...")
        
        try {
            // 按行分割，每行应该是一个文件名
            val lines = content.trim().split("\n")
            val jpgFiles = mutableListOf<String>()
            
            lines.forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty()) {
                    // 检查是否是JPG文件
                    if (trimmedLine.endsWith(".jpg", ignoreCase = true) || 
                        trimmedLine.endsWith(".jpeg", ignoreCase = true)) {
                        jpgFiles.add(trimmedLine)
                        Log.i("DataDownload", "Found JPG file: $trimmedLine")
                    } else {
                        Log.i("DataDownload", "Found non-JPG file: $trimmedLine")
                    }
                }
            }
            
            Log.i("DataDownload", "Total JPG files found: ${jpgFiles.size}")
            
            if (jpgFiles.isNotEmpty()) {
                // 开始下载所有JPG文件
                downloadAllJpgFiles(jpgFiles)
            } else {
                Log.w("DataDownload", "No JPG files found in media.config")
                runOnUiThread {
                    showDownloadError("No JPG files found in media.config")
                }
            }
            
        } catch (e: Exception) {
            Log.e("DataDownload", "Error parsing media list: ${e.message}", e)
            runOnUiThread {
                showDownloadError("Failed to parse media list: ${e.message}")
            }
        }
    }
    
    private fun downloadAllJpgFiles(jpgFiles: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.i("DataDownload", "Starting download of ${jpgFiles.size} JPG files...")
            
            var successCount = 0
            var failCount = 0
            
            for ((index, fileName) in jpgFiles.withIndex()) {
                try {
                    Log.i("DataDownload", "Downloading file ${index + 1}/${jpgFiles.size}: $fileName")
                    
                    val success = downloadSingleJpgFile(fileName)
                    if (success) {
                        successCount++
                        Log.i("DataDownload", "✓ Successfully downloaded: $fileName")
                    } else {
                        failCount++
                        Log.e("DataDownload", "✗ Failed to download: $fileName")
                    }
                    
                    // 添加小延迟避免过快请求
                    delay(500)
                    
                } catch (e: Exception) {
                    failCount++
                    Log.e("DataDownload", "Error downloading $fileName: ${e.message}", e)
                }
            }
            
            // 显示最终结果
            val message = "Download completed: $successCount successful, $failCount failed"
            Log.i("DataDownload", message)
            
            withContext(Dispatchers.Main) {
                if (failCount == 0) {
                    showDownloadSuccess("All $successCount files downloaded successfully!")
                } else {
                    showDownloadError("Download completed with errors: $successCount successful, $failCount failed")
                }
            }
        }
    }
    
    private suspend fun downloadSingleJpgFile(fileName: String): Boolean {
        return try {
            val deviceIp = getDeviceIpFromBLE() ?: return false
            val url = "http://$deviceIp/files/$fileName"
            Log.i("DataDownload", "Downloading: $url")
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val file = File(getExternalFilesDir("DCIM"), fileName)
                val outputStream = FileOutputStream(file)
                
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytes = 0L
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                }
                
                outputStream.close()
                inputStream.close()
                
                Log.i("DataDownload", "File downloaded: $fileName (${totalBytes} bytes)")
                
                // 保存到相册
                saveToAlbum(file, fileName)
                
                true
            } else {
                Log.e("DataDownload", "Failed to download $fileName. Response code: ${connection.responseCode}")
                false
            }
            
        } catch (e: Exception) {
            Log.e("DataDownload", "Error downloading $fileName: ${e.message}", e)
            false
        }
    }
    
    private fun saveToAlbum(file: File, fileName: String) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GlassesDownloads")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            
            val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { imageUri ->
                contentResolver.openOutputStream(imageUri).use { out ->
                    if (out != null) {
                        file.inputStream().use { input -> input.copyTo(out) }
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(imageUri, contentValues, null, null)
                Log.i("DataDownload", "Saved $fileName to gallery: $imageUri")
            } ?: run {
                Log.w("DataDownload", "Failed to insert media store entry for $fileName")
            }

            // Add fallback local manifest record for quick lookup
            appendAlbumMetadata(fileName, file.absolutePath)
        } catch (e: Exception) {
            Log.e("DataDownload", "Error saving to album: ${e.message}", e)
        }
    }

    private fun appendAlbumMetadata(fileName: String, filePath: String) {
        try {
            val metaFile = File(filesDir, "glasses_album.json")
            val records = if (metaFile.exists()) {
                metaFile.readText().takeIf { it.isNotBlank() } ?: "[]"
            } else {
                "[]"
            }
            
            val sanitized = records.trim().removePrefix("[").removeSuffix("]").trim()
            val newRecord = "{\"fileName\":\"${fileName}\",\"filePath\":\"${filePath}\",\"timestamp\":${System.currentTimeMillis()}}"
            val combined = if (sanitized.isBlank()) "[$newRecord]" else "[$sanitized,$newRecord]"
            metaFile.writeText(combined)
            Log.i("DataDownload", "Album metadata appended: $fileName")
        } catch (e: Exception) {
            Log.e("DataDownload", "Failed to append album metadata: ${e.message}", e)
        }
    }
    
    private fun showDownloadSuccess(message: String) {
        // Show success message to user
        Log.i("DataDownload", "SUCCESS: $message")
        // You can implement a Toast or Snackbar here
        // Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun showDownloadError(message: String) {
        // Show error message to user
        Log.e("DataDownload", "ERROR: $message")
        // You can implement a Toast or Snackbar here
        // Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun testConnection(deviceIp: String): Boolean {
        Log.i("DataDownload", "Testing connection to $deviceIp...")
        try {
            // 尝试连接到实际的媒体配置文件
            val url = URL("http://$deviceIp/files/media.config")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000 // 连接超时
            connection.readTimeout = 5000 // 读取超时
            
            val responseCode = connection.responseCode
            Log.i("DataDownload", "Connection test response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 尝试读取一小部分内容来确认连接可用
                val inputStream = connection.inputStream
                val buffer = ByteArray(1024)
                val bytesRead = inputStream.read(buffer)
                inputStream.close()
                
                Log.i("DataDownload", "Connection test successful - read $bytesRead bytes")
                return true
            }
            
            return false
        } catch (e: Exception) {
            Log.e("DataDownload", "Connection test failed: ${e.message}", e)
            return false
        }
    }

    inner class MyDeviceNotifyListener : GlassesDeviceNotifyListener() {

        @RequiresApi(Build.VERSION_CODES.O)
        override fun parseData(cmdType: Int, response: GlassesDeviceNotifyRsp) {
            when (response.loadData[6].toInt()) {
                //眼镜电量上报
                0x05 -> {
                    //当前电量
                    val battery = response.loadData[7].toInt()
                    //是否在充电
                    val changing = response.loadData[8].toInt()
                }
                //眼镜通过快捷识别
                0x02 -> {
                    if (response.loadData.size > 9 && response.loadData[9].toInt() == 0x02) {
                        //要设置识别意图：eg 请帮我看看眼前是什么，图片中的内容
                    }
                    //获取图片缩略图
                    LargeDataHandler.getInstance().getPictureThumbnails { cmdType, success, data ->
                        //请将data存入路径,jpg的图片
                    }
                }

                0x03 -> {
                    if (response.loadData[7].toInt() == 1) {
                        //眼镜启动麦克风开始说话
                    }
                }
                //ota 升级
                0x04 -> {
                    try {
                        val download = response.loadData[7].toInt()
                        val soc = response.loadData[8].toInt()
                        val nor = response.loadData[9].toInt()
                        //download 固件下载进度 soc 下载进度 nor 升级进度
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                0x0c -> {
                    //眼镜触发暂停事件，语音播报
                    if (response.loadData.size > 7 && response.loadData[7].toInt() == 1) {
                        Log.i("MyDeviceNotifyListener", "Glasses requested pause event")
                        runOnUiThread { Toast.makeText(this@MainActivity, "Glasses paused an operation", Toast.LENGTH_SHORT).show() }
                    }
                }

                0x0d -> {
                    //解除APP绑定事件
                    if (response.loadData.size > 7 && response.loadData[7].toInt() == 1) {
                        Log.i("MyDeviceNotifyListener", "Glasses requested unbind")
                        ConnectionManager.disconnect()
                        runOnUiThread { Toast.makeText(this@MainActivity, "Device unbound by glasses", Toast.LENGTH_SHORT).show() }
                    }
                }
                //眼镜内存不足事件
                0x0e -> {

                }
                //翻译暂停事件
                0x10 -> {

                }
                //眼镜音量变化事件
                0x12 -> {
                    //音乐音量
                    //最小音量
                    response.loadData[8].toInt()
                    //最大音量
                    response.loadData[9].toInt()
                    //当前音量
                    response.loadData[10].toInt()

                    //来电音量
                    //最小音量
                    response.loadData[12].toInt()
                    //最大音量
                    response.loadData[13].toInt()
                    //当前音量
                    response.loadData[14].toInt()

                    //眼镜系统音量
                    //最小音量
                    response.loadData[16].toInt()
                    //最大音量
                    response.loadData[17].toInt()
                    //当前音量
                    response.loadData[18].toInt()

                    //当前的音量模式
                    response.loadData[19].toInt()

                }
            }
        }
    }

    private fun setThemeByTime() {
        try {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val mode = if (hour in 7..18) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
            AppCompatDelegate.setDefaultNightMode(mode)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to set theme by time", e)
        }
    }
}