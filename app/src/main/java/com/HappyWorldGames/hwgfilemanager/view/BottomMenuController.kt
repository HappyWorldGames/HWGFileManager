package com.happyworldgames.hwgfilemanager.view

import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.happyworldgames.hwgfilemanager.MainActivity
import com.happyworldgames.hwgfilemanager.R
import com.happyworldgames.hwgfilemanager.data.DataBase
import com.happyworldgames.hwgfilemanager.data.FileUtils
import java.util.*

class BottomMenuController(val mainActivity: MainActivity) {
    private val bottomMenu = mainActivity.activityMain.bottomMenu
    private val scrim = mainActivity.activityMain.scrim
    private val bottomSheetBehavior = BottomSheetBehavior.from(bottomMenu)

    init {
        bottomMenu.setNavigationItemSelectedListener {
            Thread{ FileUtils.paste(mainActivity.getCurrentPosition(), it.itemId) }.start()
            openOrClose(false)

            true
        }
        scrim.setOnClickListener {
            openOrClose(false)
        }
    }
    fun openOrClose(open: Boolean) {
        bottomSheetBehavior.state = if(open) BottomSheetBehavior.STATE_EXPANDED else BottomSheetBehavior.STATE_HIDDEN
        scrim.visibility = if(open) View.VISIBLE else View.GONE
    }

    fun showClipBoardMenu() {
        val menu = bottomMenu.menu
        menu.clear()
        DataBase.clipBoardBase.forEachIndexed { index, boardData ->
            val calendar = boardData.time
            menu.add(0, index, 0, "Files ${boardData.files.size} Time:${calendar.get(Calendar.HOUR)}:${calendar.get(
                Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}")
        }
        if(menu.size() <= 0) menu.add("No elements").isEnabled = false

        openOrClose(true)
    }
    fun showProperties() {
        val menu = bottomMenu.menu
        menu.clear()
        val fileList = FileUtils.getDataItemFromIndex(mainActivity.getCurrentPosition()).selectedItems.values.toList()

        val multi = fileList.size > 1
        val file = fileList[0]

        val nameView = TextView(mainActivity)
        nameView.apply {
            text = if(!multi) file.name else "Multiple Files"
            gravity = Gravity.CENTER
            TextViewCompat.setTextAppearance(nameView, R.style.TextAppearance_AppCompat_Large)
        }

        var sizeFiles: Long = 0
        var countFiles: Long = 0
        var countFolders: Long = 0
        fileList.forEach { tempFile ->
            val temp = FileUtils.getSizeAndCountFiles(tempFile)
            sizeFiles += temp.first
            countFiles += temp.second
            countFolders += temp.third
        }

        menu.add(0, 1, 0, "").actionView = nameView

        if(!multi) menu.add(0, 1, 0, "Type: ${if(file.isFile) "File" else "Directory"}")
        menu.add(0, 1, 0, "Path: ${file.parentFile?.absolutePath}")

        if(countFolders >= 1) menu.add(0, 1, 0, "Contains: $countFiles Files $countFolders Folders")
        menu.add(0, 1, 0, "Size: ${FileUtils.humanReadableByteCountBin(sizeFiles)}($sizeFiles bytes)")

        if(!multi) {
            menu.add(0, 1, 0, "Modified: ${FileUtils.convertLongToTime(file.lastModified())}")

            menu.add(0, 1, 0, "Readable: ${file.canRead()}")
            menu.add(0, 1, 0, "Writable: ${file.canWrite()}")
            menu.add(0, 1, 0, "Hidden: ${file.isHidden}")
        }

        if(menu.size() <= 0) menu.add("No elements").isEnabled = false

        openOrClose(true)
    }
}