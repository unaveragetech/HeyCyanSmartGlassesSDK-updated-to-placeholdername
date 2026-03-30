package com.sdk.glassessdksample.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.sdk.glassessdksample.MainActivity
import com.sdk.glassessdksample.feature.ConnectionManager
import com.sdk.glassessdksample.feature.DeviceCommandService
import com.sdk.glassessdksample.ui.DeviceBindActivity

class FeatureActionFragment : Fragment() {

    companion object {
        private const val ARG_ACTION = "arg_action"
        fun newInstance(action: String): FeatureActionFragment {
            val f = FeatureActionFragment()
            f.arguments = Bundle().apply { putString(ARG_ACTION, action) }
            return f
        }
    }

    private val action by lazy { arguments?.getString(ARG_ACTION) ?: "Unknown" }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val containerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val title = TextView(requireContext()).apply {
            text = "Action: $action"
            textSize = 18f
            setPadding(0, 0, 0, 24)
        }

        val button = Button(requireContext()).apply {
            text = action
            setOnClickListener { runAction() }
        }

        containerLayout.addView(title)
        containerLayout.addView(button)
        return containerLayout
    }

    private fun runAction() {
        when (action) {
            "Connect" -> ConnectionManager.connectWithRetry { success, message ->
                Toast.makeText(requireContext(), if (success) "Connected: $message" else "Connect failed: $message", Toast.LENGTH_SHORT).show()
            }
            "Disconnect" -> {
                ConnectionManager.disconnect()
                Toast.makeText(requireContext(), "Disconnected", Toast.LENGTH_SHORT).show()
            }
            "Sync Time" -> DeviceCommandService.setTime { success, message ->
                Toast.makeText(requireContext(), if (success) "Time synced" else "Time sync failed: $message", Toast.LENGTH_SHORT).show()
            }
            "Device Info" -> DeviceCommandService.requestDeviceInfo { success, message ->
                Toast.makeText(requireContext(), if (success) message else "Info failed: $message", Toast.LENGTH_SHORT).show()
            }
            "Battery" -> DeviceCommandService.syncBattery { success, message ->
                Toast.makeText(requireContext(), if (success) message else "Battery failed: $message", Toast.LENGTH_SHORT).show()
            }
            "Volume" -> DeviceCommandService.getVolumeInfo { success, message ->
                Toast.makeText(requireContext(), if (success) message else "Volume failed: $message", Toast.LENGTH_SHORT).show()
            }
            "Data Download" -> (activity as? MainActivity)?.startDataDownload().also {
                Toast.makeText(requireContext(), "Data download started", Toast.LENGTH_SHORT).show()
            }
            "Advanced" -> {
                val features = com.sdk.glassessdksample.feature.CustomFeatureApi.listFeatures().joinToString(", ")
                Toast.makeText(requireContext(), "Custom features: $features", Toast.LENGTH_SHORT).show()
            }
            "Pairing" -> {
                val pairingIntent = Intent(requireContext(), DeviceBindActivity::class.java)
                startActivity(pairingIntent)
            }
            else -> Toast.makeText(requireContext(), "Action unavailable", Toast.LENGTH_SHORT).show()
        }
    }
}
