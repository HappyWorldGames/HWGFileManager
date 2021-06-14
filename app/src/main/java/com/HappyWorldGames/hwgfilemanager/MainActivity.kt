package com.happyworldgames.hwgfilemanager

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.happyworldgames.hwgfilemanager.data.TabsDataBase
import com.happyworldgames.hwgfilemanager.databinding.ActivityMainBinding
import com.happyworldgames.hwgfilemanager.view.viewpager.PagerAdapter
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private val activityMain by lazy{ ActivityMainBinding.inflate(layoutInflater) }
    private val adapter = PagerAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)

        setContentView(activityMain.root)
    }

    private fun startApp() {
        activityMain.pager.adapter = adapter

        TabLayoutMediator(activityMain.tabLayout, activityMain.pager) {tab, position ->
            tab.icon = when(TabsDataBase.dataBase[position].type){
                1 -> ResourcesCompat.getDrawable(resources, R.drawable.sd_card, theme)
                else -> ResourcesCompat.getDrawable(resources, R.drawable.homepage, theme)
            }
        }.attach()

        activityMain.closeTab.setOnClickListener {
            closeTab()
        }

        //activityMain.bottomAppBar.replaceMenu(R.menu.bottom_navigation_menu_files)
    }

    private fun closeTab() {
        TabsDataBase.dataBase.removeAt(activityMain.pager.currentItem)
        adapter.notifyDataSetChanged()
        if(TabsDataBase.dataBase.size <= 0) exitProcess(0)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 1){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startApp()
            }else{
                Snackbar.make(activityMain.root, "You need give permission for work app", Snackbar.LENGTH_LONG).show()
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        closeTab()
    }
}