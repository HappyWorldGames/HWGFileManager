package com.happyworldgames.filemanager.data

import com.happyworldgames.filemanager.R

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
        val tabsBase: ArrayList<TabDataItem> = arrayListOf(TabDataItem.HomeTabDataItem(R.layout.view_pager_homepage_item))
        val clipBoardBase: ArrayList<ClipBoardData> = arrayListOf()
    }
}
