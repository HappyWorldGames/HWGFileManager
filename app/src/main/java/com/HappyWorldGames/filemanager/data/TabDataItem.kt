package com.happyworldgames.filemanager.data

import java.io.File

/*
    Contains info about tab
 */
sealed class TabDataItem(open val type: Int) {

    data class HomeTabDataItem(override val type: Int): TabDataItem(type)
    data class FileTabDataItem(override val type: Int,
                               var path: String,
                               var viewType: ViewType,
                               var mode: Mode = Mode.None,
                               val selectedItems: HashMap<String, File> = hashMapOf(),
                               val searchItems: ArrayList<File> = arrayListOf(),
                               val history: ArrayList<String> = arrayListOf(path)
    ) : TabDataItem(type) {
        enum class ViewType {
            Grid, Linear
        }
        enum class Mode {
            None, Select, Search
        }
    }

}
