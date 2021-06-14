package com.happyworldgames.hwgfilemanager.data

import com.happyworldgames.hwgfilemanager.R

/*
    Содержит информацию о вкладках
    Type это ссылка на layout

    Types:
        R.layout.homepage_item
        R.layout.view_pager_item
 */
class TabsDataBase(){
    companion object {
        val dataBase: ArrayList<TabDataItem> = arrayListOf(TabDataItem(R.layout.view_pager_item, "/sdcard"))
    }
}
