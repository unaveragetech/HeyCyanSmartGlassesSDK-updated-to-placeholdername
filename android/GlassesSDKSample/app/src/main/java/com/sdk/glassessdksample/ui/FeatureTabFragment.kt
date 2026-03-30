package com.sdk.glassessdksample.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sdk.glassessdksample.databinding.FragmentFeatureTabBinding

class FeatureTabFragment : Fragment() {

    private lateinit var binding: FragmentFeatureTabBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFeatureTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val actions = listOf("Pairing", "Connect", "Disconnect", "Sync Time", "Device Info", "Battery", "Volume", "Data Download", "Advanced")
        binding.featureViewPager.adapter = object : androidx.viewpager2.adapter.FragmentStateAdapter(this) {
            override fun getItemCount(): Int = actions.size
            override fun createFragment(position: Int) = FeatureActionFragment.newInstance(actions[position])
        }

        com.google.android.material.tabs.TabLayoutMediator(binding.featureTabLayout, binding.featureViewPager) { tab, position ->
            tab.text = actions[position]
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}

