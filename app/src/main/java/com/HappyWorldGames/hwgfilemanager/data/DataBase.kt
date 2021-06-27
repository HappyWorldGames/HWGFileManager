package com.happyworldgames.hwgfilemanager.data

import com.happyworldgames.hwgfilemanager.R

/*
    Contains info about tabs and clipboard
    Type is a link on layout

    Tabs Types:
        R.layout.view_pager_homepage_item
        R.layout.view_pager_files_item

    ClipBoard Types:
        Copy
        Cut
 */
class DataBase(){
    companion object {
        val tabsBase: ArrayList<TabDataItem> = arrayListOf(TabDataItem.FileTabDataItem(R.layout.view_pager_files_item, "/sdcard", TabDataItem.FileTabDataItem.ViewType.Grid), TabDataItem.FileTabDataItem(R.layout.view_pager_files_item, "/sdcard/Download", TabDataItem.FileTabDataItem.ViewType.Grid))
        val clipBoardBase: ArrayList<ClipBoardData> = arrayListOf()
    }
}
