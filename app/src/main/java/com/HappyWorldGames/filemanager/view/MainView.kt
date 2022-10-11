package com.happyworldgames.filemanager.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.tabs.TabLayoutMediator
import com.happyworldgames.filemanager.databinding.MainBinding

class MainView(private val context: Context) {

    private val main = MainBinding.inflate(LayoutInflater.from(context))

    private val tabLayout = main.tabLayout
    private val viewPager = main.viewPagerTabs
    private val adapter = com.happyworldgames.filemanager.view.adapter.ViewPagerAdapter(viewPager)

    init {
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = "Tab ${ pos + 1 }"
        }.attach()
    }

    fun getView(): View = main.root

}