package com.happyworldgames.hwgfilemanager

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.happyworldgames.hwgfilemanager.data.DataBase
import com.happyworldgames.hwgfilemanager.data.FileUtils
import com.happyworldgames.hwgfilemanager.data.TabDataItem
import com.happyworldgames.hwgfilemanager.databinding.ActivityMainBinding
import com.happyworldgames.hwgfilemanager.view.files.FilesRecyclerViewAdapter
import com.happyworldgames.hwgfilemanager.view.viewpager.PagerAdapter
import kotlinx.coroutines.*
import java.io.File
import java.lang.reflect.Method
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    private var previousMenuIdBottomAppBar = -1

    private val activityMain by lazy{ ActivityMainBinding.inflate(layoutInflater) }
    private val bottomSheetBehavior by lazy { BottomSheetBehavior.from(activityMain.clipboardMenu) }
    private val adapter = PagerAdapter(this, object : FilesRecyclerViewAdapter.SwitchSelectModeListener() {
        override fun onSwitch(switched: Boolean) {
            replaceBottomAppBar(if(switched) R.menu.bottom_navigation_menu_files_edit else R.menu.bottom_navigation_menu_files)
        }

        override fun onEnableOrDisableBottomAppBar(enable: Boolean) {
            val menu = activityMain.bottomAppBar.menu
            if(menu.size() <= 0 && menu[0].isEnabled == enable) return
            menu.forEach {
                it.isEnabled = enable
            }
            activityMain.bottomAppBar.alpha = if(enable) 1.0f else 0.5f
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        setContentView(activityMain.root)
        openOrCloseClipBoardMenu(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        else startApp()
    }

    private fun startApp() = launch(Dispatchers.Main) {
        activityMain.pager.adapter = adapter
        activityMain.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val menuId = when (DataBase.tabsBase[position].type) {
                    R.layout.view_pager_files_item -> if (!(DataBase.tabsBase[position] as TabDataItem.FileTabDataItem).selectMode) R.menu.bottom_navigation_menu_files else R.menu.bottom_navigation_menu_files_edit
                    R.layout.view_pager_homepage_item -> R.menu.bottom_navigation_menu_homepager
                    else -> null
                }
                if (menuId != null) replaceBottomAppBar(menuId)

                DataBase.tabsBase.forEachIndexed { index, tabDataItem ->
                    if (tabDataItem is TabDataItem.FileTabDataItem && tabDataItem.selectMode) {
                        tabDataItem.selectMode = false
                        getFileManagerAdapter(index).notifyItemRangeChanged(0, getFileManagerAdapter(index).itemCount)
                    }
                }
            }
        })
        activityMain.bottomAppBar.setOnNavigationItemSelectedListener {
            when(previousMenuIdBottomAppBar) {
                R.menu.bottom_navigation_menu_files -> when(it.itemId){
                    R.id.add -> showPopupMenuAddFileOrFolder()
                    R.id.search -> TODO()
                    R.id.refresh -> refreshCurrentItem()
                    R.id.view -> TODO()
                    R.id.clip_board -> openOrCloseClipBoardMenu(true)
                }
                R.menu.bottom_navigation_menu_files_edit -> {
                    when (it.itemId) {
                        R.id.copy -> FileUtils.copy(activityMain.pager.currentItem)
                        R.id.cut -> FileUtils.cut(activityMain.pager.currentItem)
                        R.id.delete -> showAlertDelete()
                        R.id.rename -> showAlertRename()
                        R.id.more -> showPopupMenuMore()
                    }
                    when (it.itemId) {
                        R.id.copy, R.id.cut -> { getCurrentFileManagerAdapter().switchSelectMode(false); refreshCurrentItem() }
                    }
                }
                R.menu.bottom_navigation_menu_homepager -> TODO()
            }
            true
        }
        activityMain.clipboardMenu.setNavigationItemSelectedListener {
            Thread{ FileUtils.paste(activityMain.pager.currentItem, it.itemId) }.start()
            openOrCloseClipBoardMenu(false)

            true
        }

        TabLayoutMediator(activityMain.tabLayout, activityMain.pager) {tab, position ->
            tab.icon = when(DataBase.tabsBase[position].type){
                1 -> ResourcesCompat.getDrawable(resources, R.drawable.sd_card, theme)
                else -> ResourcesCompat.getDrawable(resources, R.drawable.homepage, theme)
            }
        }.attach()

        activityMain.closeTab.setOnClickListener {
            backOrCloseTab(null)
        }
        activityMain.scrim.setOnClickListener {
            openOrCloseClipBoardMenu(false)
        }
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
        launch(Dispatchers.Main) {
            val fileManagerAdapter = getCurrentFileManagerAdapter()
            if (fileManagerAdapter.isSelectMode()) fileManagerAdapter.switchSelectMode(false)
            else backOrCloseTab(fileManagerAdapter)
        }
    }
    private fun backOrCloseTab(fileManagerAdapter: FilesRecyclerViewAdapter?) = launch(Dispatchers.Main) {
        val curPos = activityMain.pager.currentItem
        if(fileManagerAdapter != null) {
            val historyItem = FileUtils.getDataItemFromIndex(curPos).history
            if(historyItem.size > 1) {
                fileManagerAdapter.goTo(File(historyItem[historyItem.lastIndex - 1]), true)
                historyItem.removeLast()
                return@launch
            }
        }
        DataBase.tabsBase.removeAt(curPos)
        adapter.notifyItemRemoved(curPos)
        if(DataBase.tabsBase.size <= 0) exitProcess(0)
    }

    private fun getFileManagerAdapter(index: Int): FilesRecyclerViewAdapter = (((activityMain.pager[0] as RecyclerView).findViewHolderForAdapterPosition(index) as PagerAdapter.FilesPageHolder).viewPagerFilesItemBinding.filesList.adapter as FilesRecyclerViewAdapter)
    private fun getCurrentFileManagerAdapter(): FilesRecyclerViewAdapter = getFileManagerAdapter(activityMain.pager.currentItem)

    fun replaceBottomAppBar(idMenu: Int) = launch(Dispatchers.Main) {
        if(previousMenuIdBottomAppBar != idMenu) {
            activityMain.bottomAppBar.menu.clear()
            activityMain.bottomAppBar.inflateMenu(idMenu)
            activityMain.bottomAppBar.alpha = 1.0f
            previousMenuIdBottomAppBar = idMenu
        }
    }

    private fun showPopupMenuAddFileOrFolder(){
        val popup = PopupMenu(this, activityMain.bottomAppBar)
        popup.apply {
            inflate(R.menu.popup_menu_add_file_or_folder)

            setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.file -> showAlertCreateFileOrFolder(true)
                    R.id.folder -> showAlertCreateFileOrFolder(false)
                }
                true
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popup.setForceShowIcon(true)
        }else{
            try {
                val fields = popup.javaClass.declaredFields
                for (field in fields) {
                    if ("mPopup" == field.name) {
                        field.isAccessible = true
                        val menuPopupHelper = field[popup]
                        val classPopupHelper =
                            Class.forName(menuPopupHelper.javaClass.name)
                        val setForceIcons: Method = classPopupHelper.getMethod(
                            "setForceShowIcon",
                            Boolean::class.javaPrimitiveType
                        )
                        setForceIcons.invoke(menuPopupHelper, true)
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        popup.show()
    }
    private fun showAlertCreateFileOrFolder(isFile: Boolean){
        val builder = AlertDialog.Builder(this)

        val editName = EditText(this)
        editName.apply {
            hint = "Enter Name"
            inputType = InputType.TYPE_CLASS_TEXT
            maxLines = 1
        }
        builder.apply {
            setTitle("New ${ if(isFile) "File" else "Folder"}")
            setView(editName)
            setPositiveButton("Create", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    if(editName.text.toString() == "" || DataBase.tabsBase[activityMain.pager.currentItem] !is TabDataItem.FileTabDataItem) return

                    val file = File((DataBase.tabsBase[activityMain.pager.currentItem] as TabDataItem.FileTabDataItem).path, editName.text.toString())
                    if(!file.parentFile!!.canWrite()) return

                    if(isFile) file.createNewFile()
                    else file.mkdirs()

                    refreshCurrentItem()
                }
            })
            setNegativeButton("Cancel", null)
        }
        builder.show()
    }

    private fun refreshCurrentItem() {
        adapter.notifyItemChanged(activityMain.pager.currentItem)
    }

    private fun openOrCloseClipBoardMenu(open: Boolean) {
        val menu = activityMain.clipboardMenu.menu
        menu.clear()
        DataBase.clipBoardBase.forEachIndexed { index, boardData ->
            val calendar = boardData.time
            menu.add(0, index, 0, "Files ${boardData.files.size} Time:${calendar.get(Calendar.HOUR)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}")
        }
        if(menu.size() <= 0) menu.add("No elements").isEnabled = false

        bottomSheetBehavior.state = if(open) BottomSheetBehavior.STATE_EXPANDED else BottomSheetBehavior.STATE_HIDDEN
        activityMain.scrim.visibility = if(open) View.VISIBLE else View.GONE
    }
    private fun showAlertDelete() {
        AlertDialog.Builder(this).apply {
            setTitle("Delete Files")
            setPositiveButton("Delete") { _, _ ->
                FileUtils.delete(activityMain.pager.currentItem)
                getCurrentFileManagerAdapter().switchSelectMode(false)
                refreshCurrentItem()
            }
            setNegativeButton("Cancel", null)
        }.show()
    }
    private fun showAlertRename() {
        val builder = AlertDialog.Builder(this)
        val oldFile = FileUtils.getDataItemFromIndex(activityMain.pager.currentItem).selectedItems.values.elementAt(0)

        val editName = EditText(this)
        editName.apply {
            hint = "Enter Name"
            setText(oldFile.name)
            inputType = InputType.TYPE_CLASS_TEXT
            maxLines = 1
        }
        builder.apply {
            setTitle("Rename")
            setView(editName)
            setPositiveButton("Rename", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    if(editName.text.toString() == "" || DataBase.tabsBase[activityMain.pager.currentItem] !is TabDataItem.FileTabDataItem) return

                    val file = File((DataBase.tabsBase[activityMain.pager.currentItem] as TabDataItem.FileTabDataItem).path, editName.text.toString())
                    if(!file.parentFile!!.canWrite()) return

                    oldFile.renameTo(file)

                    getCurrentFileManagerAdapter().switchSelectMode(false)
                    refreshCurrentItem()
                }
            })
            setNegativeButton("Cancel", null)
        }
        builder.show()
    }
    private fun showPopupMenuMore() {
        val popup = PopupMenu(this, activityMain.bottomAppBar)
        popup.apply {
            inflate(R.menu.popup_menu_files_edit_more)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) gravity = Gravity.END
            setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.file -> showAlertCreateFileOrFolder(true)
                    R.id.folder -> showAlertCreateFileOrFolder(false)
                }
                true
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popup.setForceShowIcon(true)
        }else{
            try {
                val fields = popup.javaClass.declaredFields
                for (field in fields) {
                    if ("mPopup" == field.name) {
                        field.isAccessible = true
                        val menuPopupHelper = field[popup]
                        val classPopupHelper =
                            Class.forName(menuPopupHelper.javaClass.name)
                        val setForceIcons: Method = classPopupHelper.getMethod(
                            "setForceShowIcon",
                            Boolean::class.javaPrimitiveType
                        )
                        setForceIcons.invoke(menuPopupHelper, true)
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        popup.show()
    }
}