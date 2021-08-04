package com.happyworldgames.filemanager

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.InputType
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.happyworldgames.filemanager.data.DataBase
import com.happyworldgames.filemanager.data.FileUtils
import com.happyworldgames.filemanager.data.TabDataItem
import com.happyworldgames.filemanager.databinding.ActivityMainBinding
import com.happyworldgames.filemanager.view.BottomMenuController
import com.happyworldgames.filemanager.view.files.FilesRecyclerViewAdapter
import com.happyworldgames.filemanager.view.viewpager.PagerAdapter
import kotlinx.coroutines.*
import java.io.File
import java.lang.reflect.Method
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(), CoroutineScope {
    companion object {
        @SuppressLint("StaticFieldLeak")
        var forServiceBottomMenuController: BottomMenuController? = null
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    private var previousMenuIdBottomAppBar = -1

    val activityMain by lazy{ ActivityMainBinding.inflate(layoutInflater) }
    private val bottomMenuController by lazy { BottomMenuController(this) }
    private val adapter by lazy { PagerAdapter(this, activityMain.pathButton, switchSelectModeListener)  }
    private val switchSelectModeListener = object : FilesRecyclerViewAdapter.SwitchSelectModeListener() {
        override fun onSwitch(mode: TabDataItem.FileTabDataItem.Mode) {
            if(actionMode == null) actionMode = activityMain.toolbar.startActionMode(callback)
            when(mode) {
                TabDataItem.FileTabDataItem.Mode.Select -> actionMode?.title = "1 selected"
                TabDataItem.FileTabDataItem.Mode.Search -> actionMode?.title = ""
                TabDataItem.FileTabDataItem.Mode.None -> {
                    actionMode?.finish()
                    actionMode = null
                    supportActionBar?.show()
                }
            }

            val switched = mode == TabDataItem.FileTabDataItem.Mode.Select
            replaceBottomAppBar(if(switched) R.menu.bottom_navigation_menu_files_edit else R.menu.bottom_navigation_menu_files)
        }

        override fun onEnableOrDisableBottomAppBar(enable: Boolean) {
            actionMode?.title = "${FileUtils.getDataItemFromIndex(getCurrentPosition()).selectedItems.values.size} selected"

            val menu = activityMain.bottomAppBar.menu
            if(menu.size() <= 0 && menu[0].isEnabled == enable) return
            menu.forEach {
                it.isEnabled = enable
            }
            activityMain.bottomAppBar.alpha = if(enable) 1.0f else 0.5f
        }

        override fun showSnackBar(text: String, length: Int) {
            val rootView: View = this@MainActivity.window.decorView.findViewById(android.R.id.content)
            Snackbar.make(rootView, text, length).show()
        }
    }

    var actionMode: ActionMode? = null
    val callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            if(FileUtils.getDataItemFromIndex(getCurrentPosition()).mode == TabDataItem.FileTabDataItem.Mode.Select) menuInflater.inflate(R.menu.action_mode_select, menu)
            else {
                var searchTask: Job? = null
                val searchEditText = EditText(this@MainActivity)
                searchEditText.apply {
                    inputType = InputType.TYPE_TEXT_VARIATION_FILTER
                    hint = "Search"
                    doAfterTextChanged {
                        searchTask?.cancel()
                        val dataItem = FileUtils.getDataItemFromIndex(getCurrentPosition())
                        if(searchEditText.toString() == "") {
                            dataItem.mode = TabDataItem.FileTabDataItem.Mode.None
                            getCurrentFileManagerAdapter().notifyDataSetChanged()
                            return@doAfterTextChanged
                        }else if(dataItem.mode != TabDataItem.FileTabDataItem.Mode.Search) dataItem.mode = TabDataItem.FileTabDataItem.Mode.Search

                        if(dataItem.searchItems.isNotEmpty()) dataItem.searchItems.clear()

                        searchTask = launch(Dispatchers.IO) {
                            FileUtils.search(File(dataItem.path), searchEditText.text.toString()) {
                                dataItem.searchItems.add(it)
                                launch(Dispatchers.Main) {
                                    /* Needed fix future*/
                                    getCurrentFileManagerAdapter().notifyDataSetChanged() //.notifyItemInserted(dataItem.searchItems.lastIndex)
                                }
                            }
                        }
                    }
                }
                mode.customView = searchEditText
            }
            //supportActionBar?.hide()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.select_all -> {
                    val dataItem = FileUtils.getDataItemFromIndex(getCurrentPosition())
                    dataItem.selectedItems.clear()
                    item.isChecked = !item.isChecked
                    if(item.isChecked) {
                        File(dataItem.path).listFiles()!!.forEach {
                            dataItem.selectedItems[it.absolutePath] = it
                        }
                    }

                    refreshCurrentItem()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            getCurrentFileManagerAdapter().switchMode(TabDataItem.FileTabDataItem.Mode.None)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this

        setContentView(activityMain.root)
        bottomMenuController.openOrClose(false)
        forServiceBottomMenuController = bottomMenuController
        setSupportActionBar(activityMain.toolbar)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if (Environment.isExternalStorageManager()) {
                startApp()
            } else {
                //request for the permission
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri: Uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        else startApp()
    }

    private fun startApp() = launch(Dispatchers.Main) {
        activityMain.pager.adapter = adapter
        activityMain.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val path = File(FileUtils.getDataItemFromIndex(getCurrentPosition()).path)
                activityMain.pathButton.text = (path.parentFile!!.name + "/" + path.name)

                val menuId = when (DataBase.tabsBase[position].type) {
                    R.layout.view_pager_files_item -> if (FileUtils.getDataItemFromIndex(getCurrentPosition()).mode != TabDataItem.FileTabDataItem.Mode.Select) R.menu.bottom_navigation_menu_files else R.menu.bottom_navigation_menu_files_edit
                    R.layout.view_pager_homepage_item -> R.menu.bottom_navigation_menu_homepager
                    else -> null
                }
                if (menuId != null) replaceBottomAppBar(menuId)

                DataBase.tabsBase.forEachIndexed { index, tabDataItem ->
                    if (tabDataItem is TabDataItem.FileTabDataItem && tabDataItem.mode == TabDataItem.FileTabDataItem.Mode.Select) {
                        tabDataItem.mode = TabDataItem.FileTabDataItem.Mode.None
                        getFileManagerAdapter(index).notifyItemRangeChanged(0, getFileManagerAdapter(index).itemCount)
                    }
                }
                actionMode?.finish()
            }
        })
        activityMain.bottomAppBar.setOnItemSelectedListener {
            when(previousMenuIdBottomAppBar) {
                R.menu.bottom_navigation_menu_files -> when(it.itemId){
                    R.id.add -> showPopupMenuAddFileOrFolder()
                    R.id.search -> showOrHideSearch()
                    R.id.refresh -> refreshCurrentItem()
                    R.id.view -> switchViewCurrentItem()
                    R.id.clip_board -> openOrCloseClipBoardMenu()
                }
                R.menu.bottom_navigation_menu_files_edit -> {
                    when (it.itemId) {
                        R.id.copy -> FileUtils.copy(getCurrentPosition())
                        R.id.cut -> FileUtils.cut(getCurrentPosition())
                        R.id.delete -> showDelete()
                        R.id.rename -> showRename()
                        R.id.more -> showPopupMenuMore()
                    }
                    when (it.itemId) {
                        R.id.copy, R.id.cut -> getCurrentFileManagerAdapter().switchMode(TabDataItem.FileTabDataItem.Mode.None)
                    }
                }
                R.menu.bottom_navigation_menu_homepager -> TODO()
            }
            true
        }

        TabLayoutMediator(activityMain.tabLayout, activityMain.pager) {tab, position ->
            tab.icon = when(DataBase.tabsBase[position].type){
                R.layout.view_pager_files_item -> ResourcesCompat.getDrawable(resources, R.drawable.sd_card, theme)
                else -> ResourcesCompat.getDrawable(resources, R.drawable.homepage, theme)
            }
        }.attach()

        activityMain.closeTab.setOnClickListener {
            backOrCloseTab(null)
        }
        activityMain.pathButton.setOnClickListener {
            val fileManagerAdapter = getCurrentFileManagerAdapter()
            if(fileManagerAdapter.getMode() != TabDataItem.FileTabDataItem.Mode.Select) fileManagerAdapter.goTo(fileManagerAdapter.getFilePath().parentFile!!)
            else fileManagerAdapter.switchMode(TabDataItem.FileTabDataItem.Mode.None)
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
            when(fileManagerAdapter.getMode()) {
                TabDataItem.FileTabDataItem.Mode.Select, TabDataItem.FileTabDataItem.Mode.Search -> fileManagerAdapter.switchMode(TabDataItem.FileTabDataItem.Mode.None)
                else -> backOrCloseTab(fileManagerAdapter)
            }
        }
    }
    private fun backOrCloseTab(fileManagerAdapter: FilesRecyclerViewAdapter?) = launch(Dispatchers.Main) {
        val curPos = getCurrentPosition()
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
        else {
            val path = File(FileUtils.getDataItemFromIndex(getCurrentPosition()).path)
            activityMain.pathButton.text = (path.parentFile!!.name + "/" + path.name)
        }
    }

    private fun getFileManagerAdapter(index: Int): FilesRecyclerViewAdapter = (((activityMain.pager[0] as RecyclerView).findViewHolderForAdapterPosition(index) as PagerAdapter.FilesPageHolder).viewPagerFilesItemBinding.filesList.adapter as FilesRecyclerViewAdapter)
    fun getCurrentFileManagerAdapter(): FilesRecyclerViewAdapter = getFileManagerAdapter(getCurrentPosition())
    fun getCurrentPosition(): Int = activityMain.pager.currentItem

    fun replaceBottomAppBar(idMenu: Int) = launch(Dispatchers.Main) {
        if(previousMenuIdBottomAppBar != idMenu) {
            activityMain.bottomAppBar.menu.clear()
            activityMain.bottomAppBar.inflateMenu(idMenu)
            activityMain.bottomAppBar.alpha = 1.0f
            previousMenuIdBottomAppBar = idMenu
        }
    }

    private fun showPopupMenuAddFileOrFolder(){
        bottomMenuController.showCreateFileOrFolder()
        /*val popup = PopupMenu(this, activityMain.bottomAppBar)
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

        popup.show()*/
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
                    if(editName.text.toString() == "" || DataBase.tabsBase[getCurrentPosition()] !is TabDataItem.FileTabDataItem) return

                    val file = File(FileUtils.getDataItemFromIndex(getCurrentPosition()).path, editName.text.toString())
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
    private fun showOrHideSearch() {
        val fileManagerAdapter = getCurrentFileManagerAdapter()
        fileManagerAdapter.switchMode(if(fileManagerAdapter.getMode() != TabDataItem.FileTabDataItem.Mode.Search) TabDataItem.FileTabDataItem.Mode.Search else TabDataItem.FileTabDataItem.Mode.None)
    }
    fun refreshCurrentItem() {
        adapter.notifyItemChanged(getCurrentPosition())
    }
    private fun switchViewCurrentItem() {
        val viewType = FileUtils.getDataItemFromIndex(getCurrentPosition()).viewType
        FileUtils.getDataItemFromIndex(getCurrentPosition()).viewType = if(viewType == TabDataItem.FileTabDataItem.ViewType.Grid) TabDataItem.FileTabDataItem.ViewType.Linear else TabDataItem.FileTabDataItem.ViewType.Grid

        refreshCurrentItem()
    }
    private fun openOrCloseClipBoardMenu() {
        bottomMenuController.showClipBoardMenu()
    }

    private fun showDelete() {
        bottomMenuController.showDelete()
    }
    private fun showRename() {
        bottomMenuController.showRename()
    }
    private fun showPopupMenuMore() {
        val popup = PopupMenu(this, activityMain.bottomAppBar)
        popup.apply {
            inflate(R.menu.popup_menu_files_edit_more)

            val selectItems = FileUtils.getDataItemFromIndex(getCurrentPosition()).selectedItems.values.toList()
            if(selectItems.size == 1 && FileUtils.checkIfFileHasExtension(selectItems[0].name, FileUtils.archiveExtensions)) menu.findItem(R.id.uncompress).isVisible = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) gravity = Gravity.END
            setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.share -> showShare()
                    R.id.compress -> showCompress()
                    R.id.uncompress -> showUnCompress()
                    R.id.properties -> showProperties()
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

    private fun showShare() {
        try{
            FileUtils.shareMultiple(this@MainActivity, FileUtils.getDataItemFromIndex(getCurrentPosition()).selectedItems.values.toList())
        }catch (e: Throwable) {
            switchSelectModeListener.showSnackBar(if(e.toString().contains("Many items (>100)")) "Many items (>100)" else e.toString(), Snackbar.LENGTH_LONG)
        }
    }
    private fun showCompress() {
        bottomMenuController.showCompress()
    }
    private fun showUnCompress() {
        bottomMenuController.showExtractTo()
    }
    private fun showProperties() {
        bottomMenuController.showProperties()
    }

    fun hideKeyboard() {
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(this)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}