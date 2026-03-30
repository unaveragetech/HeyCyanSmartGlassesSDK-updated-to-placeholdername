package com.sdk.glassessdksample.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> DeviceTabFragment()
        1 -> FeatureTabFragment()
        else -> throw IllegalStateException("Invalid tab position")
    }
}
