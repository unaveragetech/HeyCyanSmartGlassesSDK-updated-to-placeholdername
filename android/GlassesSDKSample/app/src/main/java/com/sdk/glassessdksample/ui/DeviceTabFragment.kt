package com.sdk.glassessdksample.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.sdk.glassessdksample.databinding.FragmentDeviceTabBinding
import com.sdk.glassessdksample.ui.hasBluetooth
import com.sdk.glassessdksample.ui.requestBluetoothPermission
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions

class DeviceTabFragment : Fragment() {

    private var _binding: FragmentDeviceTabBinding? = null
    private val binding get() = _binding!!
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var adapter: DeviceAdapter
    private val discoveredDevices = mutableListOf<BluetoothDevice>()

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (!discoveredDevices.any { dev -> dev.address == it.address }) {
                            discoveredDevices.add(it)
                            adapter.submitList(discoveredDevices.toList())
                            binding.tvBtStatus.text = "Found ${discoveredDevices.size} devices (including paired)"
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    binding.tvBtStatus.text = "Discovery complete: ${discoveredDevices.size} devices"
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val connected = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    connected?.let {
                        binding.tvBtStatus.text = "Connected: ${it.name ?: it.address}"
                        loadBondedDevices()
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val disconnected = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    disconnected?.let {
                        binding.tvBtStatus.text = "Disconnected: ${it.name ?: it.address}"
                        loadBondedDevices()
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDeviceTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: run {
            binding.tvBtStatus.text = "Bluetooth not supported"
            throw IllegalStateException("Bluetooth not supported")
        }

        adapter = DeviceAdapter(discoveredDevices) { device ->
            Toast.makeText(requireContext(), "Selected: ${device.name ?: "Unnamed"}", Toast.LENGTH_SHORT).show()
        }

        binding.rvDevices.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDevices.adapter = adapter

        binding.btnScanBt.setOnClickListener {
            if (!hasBluetooth(requireActivity())) {
                requestBluetoothPermission(requireActivity(), object : OnPermissionCallback {
                    override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                        if (all) {
                            checkAndStartScan()
                        } else {
                            binding.tvBtStatus.text = "Bluetooth permission not fully granted"
                        }
                    }

                    override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                        if (never) {
                            XXPermissions.startPermissionActivity(requireActivity(), permissions)
                        }
                        binding.tvBtStatus.text = "Permission denied"
                    }
                })
            } else {
                checkAndStartScan()
            }
        }

        binding.btnCheckBt.setOnClickListener {
            val info = StringBuilder()
            info.append("Adapter enabled: ${bluetoothAdapter.isEnabled}\n")
            info.append("Adapter discovering: ${bluetoothAdapter.isDiscovering}\n")
            info.append("Bonded devices: ${bluetoothAdapter.bondedDevices.size}\n")
            info.append("Emulator device: ${isEmulator()}\n")
            binding.tvBtStatus.text = info.toString()
        }

        binding.btnOpenPairing.setOnClickListener {
            startActivity(Intent(requireContext(), DeviceBindActivity::class.java))
        }

        binding.btnSkipPairing.setOnClickListener {
            binding.tvBtStatus.text = "Pairing bypassed: you can still use local UI and voice features."
            Toast.makeText(requireContext(), "Skipping pairing mode.", Toast.LENGTH_SHORT).show()
        }

        loadBondedDevices()
        updateStatus()
    }

    private fun checkAndStartScan() {
        if (!bluetoothAdapter.isEnabled) {
            binding.tvBtStatus.text = "Bluetooth is off. Please enable in system settings."
            return
        }

        startBtScan()
    }

    private fun startBtScan() {
        discoveredDevices.clear()
        adapter.submitList(discoveredDevices.toList())

        loadBondedDevices()

        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        requireContext().registerReceiver(scanReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        requireContext().registerReceiver(scanReceiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
        requireContext().registerReceiver(scanReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED))
        requireContext().registerReceiver(scanReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))

        if (bluetoothAdapter.startDiscovery()) {
            binding.tvBtStatus.text = "Scanning nearby Bluetooth devices..."
        } else {
            binding.tvBtStatus.text = "Discovery failed (emulator may not support Bluetooth). Showing paired devices."
        }
    }

    private fun loadBondedDevices() {
        val bonded = bluetoothAdapter.bondedDevices
            .sortedBy { (it.name ?: it.address).lowercase() }
            .toList()

        if (bonded.isNotEmpty()) {
            discoveredDevices.clear()
            discoveredDevices.addAll(bonded)
            adapter.submitList(discoveredDevices.toList())
            binding.tvBtStatus.text = "Paired devices: ${bonded.size} (showing now)"

            val connected = mutableListOf<String>()
            val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val connectedProfiles = listOf(BluetoothProfile.HEADSET, BluetoothProfile.A2DP, BluetoothProfile.GATT)
            connectedProfiles.forEach { profile ->
                val profileDevices = bluetoothManager.getConnectedDevices(profile)
                profileDevices.forEach { connected.add(it.name ?: it.address) }
            }

            if (connected.isNotEmpty()) {
                binding.tvBtStatus.text = "Paired: ${bonded.size} • Connected: ${connected.joinToString()}"
            }
        } else {
            binding.tvBtStatus.text = "No paired devices."
            adapter.submitList(listOf())
        }
    }

    private fun updateStatus() {
        val hasHostBt = isEmulator().not()
        val status = if (!bluetoothAdapter.isEnabled) {
            "Bluetooth is disabled"
        } else if (!hasHostBt) {
            "Emulator may not expose host BT; use physical device for real hardware scanning"
        } else {
            "Bluetooth is enabled"
        }
        binding.tvBtStatus.text = status
    }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            requireContext().unregisterReceiver(scanReceiver)
        } catch (e: Exception) {
        }
        _binding = null
    }
}
