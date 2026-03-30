package com.sdk.glassessdksample.ui
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.communication.CommandHandle
import com.oudmon.ble.base.communication.req.SimpleKeyReq
import com.oudmon.ble.base.scan.BleScannerHelper
import com.oudmon.ble.base.scan.ScanRecord
import com.oudmon.ble.base.scan.ScanWrapperCallback
import com.sdk.glassessdksample.R
import com.sdk.glassessdksample.databinding.ActivityDeviceBindBinding
import com.xiasuhuei321.loadingdialog.view.LoadingDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DeviceBindActivity : BaseActivity() {
    private lateinit var binding: ActivityDeviceBindBinding
    private lateinit var  adapter: DeviceListAdapter
    private var scanSize:Int=0
    private val runnable=MyRunnable()

    private var loadingDialog: LoadingDialog? = null
    private val myHandler : Handler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }

    val deviceList = mutableListOf<SmartWatch>()
    val bleScanCallback: BleCallback = BleCallback()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityDeviceBindBinding.inflate(layoutInflater)
        EventBus.getDefault().register(this)
        setContentView(binding.root)
        
        // Add a reminder to the user
        Toast.makeText(this, "Ensure devices are NOT connected to PC/other phones and are in pairing mode.", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        // Request Bluetooth permissions first, then scan
        if (hasBluetooth(this)) {
            startScanningFlow()
        } else {
            requestBluetoothPermission(this, object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                    if (all) startScanningFlow()
                }
                override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                    if (never) XXPermissions.startPermissionActivity(this@DeviceBindActivity, permissions)
                }
            })
        }
    }

    private fun startScanningFlow() {
        if (BluetoothUtils.isEnabledBluetooth(this)) {
            binding.startScan.performClick()
        } else {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }
            startActivityForResult(intent, 300)
        }
    }

    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(this.packageName)
        }
        return false
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(messageEvent: BluetoothEvent) {
        Log.i(TAG, "onMessageEvent: " + messageEvent.connect)
        if (messageEvent.connect) {
            loadingDialog?.close()
            finish()
        } else {
            loadingDialog?.close()
            showPairingFailedScreen()
        }
    }

    private fun showPairingFailedScreen() {
        Toast.makeText(this, "Pairing failed. Ensure the glasses are in pairing mode.", Toast.LENGTH_LONG).show()
    }

    override fun setupViews() {
        super.setupViews()
        adapter = DeviceListAdapter(this, deviceList)
        binding.run {
            deviceRcv.layoutManager = LinearLayoutManager(this@DeviceBindActivity)
            deviceRcv.adapter = adapter
            titleBar.tvTitle.text="Scan Devices"
            titleBar.ivNavigateBefore.setOnClickListener {
                finish()
            }
        }

        adapter.notifyDataSetChanged()

        adapter.run {
            setOnItemClickListener{ _, _, position->
                myHandler.removeCallbacks(runnable)
                val smartWatch:SmartWatch= deviceList[position]
                BleOperateManager.getInstance().connectDirectly(smartWatch.deviceAddress)

                loadingDialog = LoadingDialog(this@DeviceBindActivity)
                loadingDialog?.setLoadingText(getString(R.string.text_22))
                    ?.show()
            }
        }

        setOnClickListener(binding.startScan) {
            if (!hasBluetooth(this@DeviceBindActivity)) {
                requestBluetoothPermission(this@DeviceBindActivity, object : OnPermissionCallback {
                    override fun onGranted(p: MutableList<String>, all: Boolean) { if(all) binding.startScan.performClick() }
                })
                return@setOnClickListener
            }

            deviceList.clear()
            adapter.notifyDataSetChanged()
            BleScannerHelper.getInstance().reSetCallback()
            
            if(!BluetoothUtils.isEnabledBluetooth(this@DeviceBindActivity)){
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(intent, 300)
            }else{
                scanSize = 0
                BleScannerHelper.getInstance()
                    .scanDevice(this@DeviceBindActivity, null, bleScanCallback)
                myHandler.removeCallbacks(runnable)
                myHandler.postDelayed(runnable, 15 * 1000)
            }
        }
    }

    inner class MyRunnable:Runnable{
        override fun run() {
            BleScannerHelper.getInstance().stopScan(this@DeviceBindActivity)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    inner class BleCallback : ScanWrapperCallback {
        override fun onStart() {
            Log.i("BleCallback", "Scan started")
        }

        override fun onStop() {
            Log.i("BleCallback", "Scan stopped")
        }

        override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
            if (device != null && (!device.name.isNullOrEmpty())) {
                val smartWatch = SmartWatch(device.name, device.address, rssi)
                Log.i("BleCallback", "Found: ${device.name} - ${device.address}")

                if (!deviceList.contains(smartWatch)) {
                    scanSize++
                    deviceList.add(0, smartWatch)
                    deviceList.sortByDescending { it.rssi }
                    adapter.notifyDataSetChanged()
                    if (scanSize > 50) {
                        BleScannerHelper.getInstance().stopScan(this@DeviceBindActivity)
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleCallback", "Scan failed with code: $errorCode")
        }

        override fun onParsedData(device: BluetoothDevice?, scanRecord: ScanRecord?) {
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
        }
    }
}