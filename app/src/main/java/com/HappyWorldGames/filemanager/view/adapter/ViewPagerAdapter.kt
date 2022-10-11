package com.happyworldgames.filemanager.view.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.flexbox.*
import com.happyworldgames.filemanager.R
import com.happyworldgames.filemanager.databinding.*
import com.happyworldgames.filemanager.view.tabs.FilesTabData
import com.happyworldgames.filemanager.view.tabs.MainTabData
import com.happyworldgames.filemanager.view.tabs.TabData
import kotlin.system.exitProcess

class ViewPagerAdapter(private val viewPager: ViewPager2) : RecyclerView.Adapter<ViewPagerAdapter.BaseTabViewHolder>() {
    private val tabs = mutableListOf<TabData>(MainTabData())

    fun addTab(tab: TabData) {
        tabs.add(tab)
        notifyItemInserted(tabs.lastIndex)
        viewPager.currentItem = tabs.lastIndex
    }
    fun removeTabAt(index: Int) {
        tabs.removeAt(index)
        notifyItemRemoved(index)
        checkTabCountForExit()
    }
    fun getTab(index: Int) = tabs[index]

    private fun checkTabCountForExit() {
        if (tabs.size <= 0) exitProcess(1)
    }

    override fun getItemViewType(position: Int): Int {
        return when (tabs[position]) {
            is MainTabData -> R.layout.main_tab
            is FilesTabData -> R.layout.files_tab
            else -> throw Exception("IDK what this")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseTabViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.base_tab, parent, false) as ConstraintLayout

        val baseTabViewHolder = when (viewType) {
            R.layout.main_tab -> MainTabViewHolder(view)
            R.layout.files_tab -> FilesTabViewHolder(view)
            else -> throw Exception("IDK what this")
        }
        baseTabViewHolder.onCreateView(parent, viewType)

        return baseTabViewHolder
    }

    override fun onBindViewHolder(holder: BaseTabViewHolder, position: Int) {
        holder.onBind(this, position)
    }

    override fun getItemCount(): Int = tabs.size

    abstract class BaseTabViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        protected val context: Context = view.context
        private val baseTab = BaseTabBinding.bind(view)

        val toolBar = baseTab.toolbar
        val emptyView = baseTab.emptyView

        open fun onCreateView(parent: ViewGroup, viewType: Int) {
            baseTab.emptyView.addView(LayoutInflater.from(context).inflate(viewType, baseTab.root, false))
            baseTab.toolbar.menu.add(0, 1, 0, "Close Tab").setIcon(android.R.drawable.ic_menu_close_clear_cancel).setShowAsAction(1)
        }

        open fun onBind(viewPagerAdapter: ViewPagerAdapter, position: Int) {
            toolBar.title = viewPagerAdapter.getTab(position).name
            toolBar.setOnMenuItemClickListener { menuItem ->
                return@setOnMenuItemClickListener when (menuItem.itemId) {
                    1 -> {
                        viewPagerAdapter.removeTabAt(position)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    class MainTabViewHolder(view: View) : BaseTabViewHolder(view) {
        private val mainTabBinding by lazy { MainTabBinding.bind(emptyView.getChildAt(0)) }

        override fun onBind(viewPagerAdapter: ViewPagerAdapter, position: Int) {
            super.onBind(viewPagerAdapter, position)

            mainTabBinding.storageView.layoutManager = LinearLayoutManager(context)
            mainTabBinding.storageView.adapter = StorageViewAdapter(context, viewPagerAdapter)
        }
    }

    class FilesTabViewHolder(view: View) : BaseTabViewHolder(view) {
        private val filesTabBinding by lazy { FilesTabBinding.bind(emptyView.getChildAt(0)) }

        @SuppressLint("UseCompatLoadingForDrawables")
        override fun onCreateView(parent: ViewGroup, viewType: Int) {
            super.onCreateView(parent, viewType)

            val layoutButton = LayoutInflater.from(context).inflate(R.layout.button_toolbar_back, parent, false)
            toolBar.addView(layoutButton)

            filesTabBinding.filesToolbar.overflowIcon = ContextCompat.getDrawable(context, R.drawable.more)
            //filesTabBinding.filesToolbar.inflateMenu(R.menu.menu_files_base)
            filesTabBinding.filesToolbar.addView(
                LayoutInflater.from(context).inflate(R.layout.toolbar_files_base, filesTabBinding.filesToolbar, false)
            )
        }

        override fun onBind(viewPagerAdapter: ViewPagerAdapter, position: Int) {
            super.onBind(viewPagerAdapter, position)
            val context = filesTabBinding.root.context
            val tabInfo = viewPagerAdapter.getTab(position) as FilesTabData

            val buttonToolbarBackBinding = ButtonToolbarBackBinding.bind(toolBar.findViewById(R.id.layout_button))
            buttonToolbarBackBinding.backButton.text = tabInfo.path

            val layoutManager = FlexboxLayoutManager(context).apply {
                justifyContent = JustifyContent.CENTER
                alignItems = AlignItems.CENTER
                flexDirection = FlexDirection.ROW
                flexWrap = FlexWrap.WRAP
            }
            filesTabBinding.filesView.layoutManager = layoutManager
            filesTabBinding.filesView.adapter = FilesViewAdapter(tabInfo, buttonToolbarBackBinding.backButton)

            val toolbarFilesBaseBinding = ToolbarFilesBaseBinding.bind(filesTabBinding.filesToolbar.findViewById(R.id.toolbar_files_base))
            toolbarFilesBaseBinding.buttonNew.setOnClickListener {
                /*val data = workDataOf(FilesWorker.code to FilesWorkerRequest.CreateFile.name, FilesWorkerRequest.CreateFile.name to "/sdcard/test.test")
                FilesWorker.startFilesWorker(context, data)*/
            }
        }
    }
}