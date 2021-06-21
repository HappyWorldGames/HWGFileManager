package com.happyworldgames.hwgfilemanager.data

import java.io.File
import java.util.*

data class ClipBoardData(val type: Type, val files: Map<String, File>){
    val time: Calendar = Calendar.getInstance()

    enum class Type {
        COPY, CUT
    }
}
