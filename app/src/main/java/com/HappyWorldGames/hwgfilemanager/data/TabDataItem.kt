package com.happyworldgames.hwgfilemanager.data

import java.io.File

/*
    Contains info about tab
 */
sealed class TabDataItem(open val type: Int) {

    data class HomeTabDataItem(override val type: Int): TabDataItem(type)
    data class FileTabDataItem(override val type: Int, var path: String, var selectMode: Boolean = false, val selectedItems: HashMap<String, File> = hashMapOf(), val history: ArrayList<String> = arrayListOf(path)) : TabDataItem(type)

}
