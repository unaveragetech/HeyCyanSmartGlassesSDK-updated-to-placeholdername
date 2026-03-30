package com.sdk.glassessdksample.ui

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sdk.glassessdksample.databinding.RecycleviewItemDeviceBinding

class DeviceAdapter(
    private val devices: MutableList<BluetoothDevice>,
    private val onSelect: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(val binding: RecycleviewItemDeviceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = RecycleviewItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.binding.rcvDeviceName.text = device.name ?: "Unknown device"
        holder.binding.rcvDeviceAddress.text = device.address
        holder.binding.root.setOnClickListener { onSelect(device) }
    }

    override fun getItemCount(): Int = devices.size

    fun submitList(newDevices: List<BluetoothDevice>) {
        devices.clear()
        devices.addAll(newDevices)
        notifyDataSetChanged()
    }
}
