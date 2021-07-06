package com.happyworldgames.filemanager.view

import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.happyworldgames.filemanager.MainActivity
import com.happyworldgames.filemanager.R
import com.happyworldgames.filemanager.data.DataBase
import com.happyworldgames.filemanager.data.FileUtils
import com.happyworldgames.filemanager.data.TabDataItem
import com.happyworldgames.filemanager.databinding.BottomMenuCompressToBinding
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import kotlin.coroutines.CoroutineContext

class BottomMenuController(private val mainActivity: MainActivity) : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    object GroupId {
        const val PASTE = 0x01
        const val DELETE = 0x02
        const val COMPRESS = 0x03
        const val EXTRACT_TO = 0X04
        const val REQUEST_OVERWRITE = 0x05
    }
    object Id {
        const val YES = 0x01
        const val NO = 0x02
    }
    
    private val bottomMenu = mainActivity.activityMain.bottomMenu
    private val scrim = mainActivity.activityMain.scrim
    private val bottomSheetBehavior = BottomSheetBehavior.from(bottomMenu)

    private lateinit var bottomMenuCompressTo: BottomMenuCompressToBinding
    private lateinit var requestOverWrite: (result: Int) -> Unit

    init {
        bottomMenu.setNavigationItemSelectedListener {
            when(it.groupId){
                GroupId.PASTE -> {
                    launch(Dispatchers.IO) {
                        FileUtils.paste(mainActivity.getCurrentPosition(), it.itemId) { file, requestWrite ->
                            runBlocking {
                                withContext(Dispatchers.Main) {
                                    requestOverWrite = requestWrite
                                    showRequestOverWrite(file)
                                }
                            }
                        }
                    }
                    mainActivity.getCurrentFileManagerAdapter().switchMode(TabDataItem.FileTabDataItem.Mode.None)
                }
                GroupId.DELETE -> {
                    when(it.itemId) {
                        Id.YES -> {
                            FileUtils.delete(mainActivity.getCurrentPosition())
                            mainActivity.getCurrentFileManagerAdapter().switchMode(TabDataItem.FileTabDataItem.Mode.None)
                        }
                    }
                }
                GroupId.COMPRESS -> {
                    when(it.itemId) {
                        Id.YES -> {
                            val name = bottomMenuCompressTo.name.text.toString()

                            val dataItem = FileUtils.getDataItemFromIndex(mainActivity.getCurrentPosition())

                            val path = when(bottomMenuCompressTo.pathChoise.checkedRadioButtonId) {
                                R.id.custom_location -> bottomMenuCompressTo.path.text.toString()
                                else -> dataItem.path
                            }

                            when(bottomMenuCompressTo.formatChoise.checkedRadioButtonId) {
                                R.id.zip -> mainActivity.launch(Dispatchers.IO){ FileUtils.zip(dataItem.selectedItems.values.toList(), File(path, name + if(!name.endsWith(".zip")) ".zip" else "")) }
                                R.id.seven_z -> TODO()
                            }

                            mainActivity.getCurrentFileManagerAdapter().switchMode(TabDataItem.FileTabDataItem.Mode.None)
                        }
                    }
                }
                GroupId.EXTRACT_TO -> {
                    when(it.itemId) {
                        Id.YES -> {
                            val dataItem = FileUtils.getDataItemFromIndex(mainActivity.getCurrentPosition())

                            val path = when(bottomMenuCompressTo.pathChoise.checkedRadioButtonId) {
                                R.id.current_location_folder -> File(dataItem.path, dataItem.selectedItems.values.first().nameWithoutExtension).absolutePath
                                R.id.custom_location -> bottomMenuCompressTo.path.text.toString()
                                else -> dataItem.path
                            }

                            FileUtils.unZip(dataItem.selectedItems.values.first(), File(path))

                            mainActivity.getCurrentFileManagerAdapter().switchMode(TabDataItem.FileTabDataItem.Mode.None)
                        }
                    }
                }
                GroupId.REQUEST_OVERWRITE -> {
                    when(it.itemId) {
                        Id.YES -> {
                            requestOverWrite(1)
                            mainActivity.getCurrentFileManagerAdapter().switchMode(TabDataItem.FileTabDataItem.Mode.None)
                        }
                        Id.NO -> {
                            requestOverWrite(0)
                            mainActivity.getCurrentFileManagerAdapter().switchMode(TabDataItem.FileTabDataItem.Mode.None)
                        }
                    }
                }
            }
            openOrClose(false)
            true
        }
        scrim.setOnClickListener {
            openOrClose(false)
        }
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) openOrClose(false, fromCallBack = true)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

        })
    }

    fun openOrClose(open: Boolean, fromCallBack: Boolean = false) {
        mainActivity.activityMain.bottomAppBar.visibility = if(open) View.GONE else View.VISIBLE
        scrim.visibility = if(open) View.VISIBLE else View.GONE
        if(!fromCallBack) bottomSheetBehavior.state = if(open) BottomSheetBehavior.STATE_EXPANDED else BottomSheetBehavior.STATE_HIDDEN
    }

    private fun getClearMenu(): Menu {
        val menu = bottomMenu.menu
        menu.clear()
        return menu
    }
    private fun clearHeader() {
        for(i in 0..bottomMenu.headerCount){
            bottomMenu.removeHeaderView(bottomMenu.getHeaderView(i))
        }
    }
    private fun clearHeaderAndMenu(): Menu {
        clearHeader()
        return getClearMenu()
    }

    fun showClipBoardMenu() {
        val menu = clearHeaderAndMenu()

        DataBase.clipBoardBase.forEachIndexed { index, boardData ->
            val calendar = boardData.time
            menu.add(GroupId.PASTE, index, 0, "Files ${boardData.files.size} Time:${calendar.get(Calendar.HOUR)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}")
        }
        if(menu.size() <= 0) menu.add("No elements").isEnabled = false

        openOrClose(true)
    }

    private fun showRequestOverWrite(file: File) {
        val menu = clearHeaderAndMenu()

        menu.add("Request overwrite").isEnabled = false
        menu.add("File: \"${file.name}\"").isEnabled = false
        menu.add(GroupId.REQUEST_OVERWRITE, Id.YES, 0, "Rewrite")
        menu.add(GroupId.REQUEST_OVERWRITE, Id.NO, 0, "Cancel")

        openOrClose(true)
    }

    fun showDelete() {
        val menu = clearHeaderAndMenu()

        menu.add("Delete Files").isEnabled = false
        menu.add(GroupId.DELETE, Id.YES, 0, "Delete")
        menu.add(GroupId.DELETE, Id.NO, 0, "Cancel")

        openOrClose(true)
    }
    fun showProperties() {
        val menu = clearHeaderAndMenu()

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

        openOrClose(true)
    }
    fun showCompress() {
        val menu = clearHeaderAndMenu()
        bottomMenuCompressTo = BottomMenuCompressToBinding.bind(bottomMenu.inflateHeaderView(R.layout.bottom_menu_compress_to))
        val dataItem = FileUtils.getDataItemFromIndex(mainActivity.getCurrentPosition())

        bottomMenuCompressTo.name.doOnTextChanged { _, _, _, _ ->
            if(menu.findItem(Id.YES).isEnabled != bottomMenuCompressTo.name.text!!.isNotEmpty()) menu.findItem(Id.YES).isEnabled = bottomMenuCompressTo.name.text!!.isNotEmpty()
        }
        bottomMenuCompressTo.name.post {
            bottomMenuCompressTo.name.setText(dataItem.selectedItems.values.first().nameWithoutExtension)
        }
        bottomMenuCompressTo.pathChoise.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId) {
                R.id.current_location -> bottomMenuCompressTo.path.isEnabled = false
                R.id.custom_location -> bottomMenuCompressTo.path.isEnabled = true
            }
        }
        bottomMenuCompressTo.path.setText(dataItem.path)
        bottomMenuCompressTo.path.setOnClickListener {
            FileUtils.showChoiceFileManager(mainActivity, File(FileUtils.getDataItemFromIndex(mainActivity.getCurrentPosition()).path)) {
                bottomMenuCompressTo.path.setText(it)
            }
        }

        menu.add(GroupId.COMPRESS, Id.YES, 0, "Compress").isEnabled = false
        menu.add(GroupId.COMPRESS, Id.NO, 0, "Cancel")

        openOrClose(true)
    }
    fun showExtractTo() {
        val menu = clearHeaderAndMenu()
        bottomMenuCompressTo = BottomMenuCompressToBinding.bind(bottomMenu.inflateHeaderView(R.layout.bottom_menu_compress_to))
        val dataItem = FileUtils.getDataItemFromIndex(mainActivity.getCurrentPosition())

        bottomMenuCompressTo.textName.visibility = View.GONE
        bottomMenuCompressTo.formatChoise.visibility = View.GONE
        bottomMenuCompressTo.currentLocationFolder.visibility = View.VISIBLE

        bottomMenuCompressTo.pathChoise.check(R.id.current_location_folder)
        bottomMenuCompressTo.pathChoise.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId) {
                R.id.current_location, R.id.current_location_folder -> bottomMenuCompressTo.path.isEnabled = false
                R.id.custom_location -> bottomMenuCompressTo.path.isEnabled = true
            }
        }
        bottomMenuCompressTo.currentLocationFolder.text = dataItem.selectedItems.values.first().nameWithoutExtension
        bottomMenuCompressTo.path.setText(dataItem.path)
        bottomMenuCompressTo.path.setOnClickListener {
            FileUtils.showChoiceFileManager(mainActivity, File(FileUtils.getDataItemFromIndex(mainActivity.getCurrentPosition()).path)) {
                bottomMenuCompressTo.path.setText(it)
            }
        }

        menu.add(GroupId.EXTRACT_TO, Id.YES, 0, "Extract To")
        menu.add(GroupId.EXTRACT_TO, Id.NO, 0, "Cancel")

        openOrClose(true)
    }
}