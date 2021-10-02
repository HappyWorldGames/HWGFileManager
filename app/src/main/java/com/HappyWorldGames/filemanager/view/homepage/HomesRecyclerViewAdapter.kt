package com.happyworldgames.filemanager.view.homepage

import android.os.StatFs
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.happyworldgames.filemanager.R
import com.happyworldgames.filemanager.data.DataBase
import com.happyworldgames.filemanager.data.FileUtils
import com.happyworldgames.filemanager.data.TabDataItem
import com.happyworldgames.filemanager.databinding.RecyclerviewItemHomesStorageBinding
import com.happyworldgames.filemanager.view.viewpager.PagerAdapter
import java.io.File

class HomesRecyclerViewAdapter(private val pagerAdapter: PagerAdapter) : RecyclerView.Adapter<HomesRecyclerViewAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_item_homes_storage, parent, false)
        return StorageViewHolder(itemView, pagerAdapter)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = getItems().size

    private fun getItems(): ArrayList<File> {
        val file = File("/storage").listFiles()!!
        val files = ArrayList<File>()
        file.forEach {
            if(it.name != "self") if(it.name == "emulated") files.add(0, File("/storage/emulated/0"))
            else files.add(it)
        }
        return files
    }

    inner class StorageViewHolder(itemView: View, pagerAdapter: PagerAdapter) : MyViewHolder(itemView) {
        private val recyclerviewItemHomesStorageBinding = RecyclerviewItemHomesStorageBinding.bind(itemView)

        override fun bind(position: Int) {
            val dataItem = getItems()

            recyclerviewItemHomesStorageBinding.root.setOnClickListener {
                DataBase.tabsBase.add(TabDataItem.FileTabDataItem(R.layout.view_pager_files_item, dataItem[position].absolutePath, TabDataItem.FileTabDataItem.ViewType.Grid))
                val pos = DataBase.tabsBase.size - 1
                pagerAdapter.notifyItemInserted(pos)
                pagerAdapter.activityMainBinding.pager.currentItem = pos
            }
            recyclerviewItemHomesStorageBinding.sdName.text = dataItem[position].name
            val statFs = StatFs(dataItem[position].absolutePath)
            val busySize = statFs.totalBytes - statFs.availableBytes
            val txt = "${FileUtils.humanReadableByteCountBin(busySize)} / ${FileUtils.humanReadableByteCountBin(statFs.totalBytes)}"
            recyclerviewItemHomesStorageBinding.sdSize.text = txt
            recyclerviewItemHomesStorageBinding.sdSizeProgress.max = (statFs.totalBytes / 1024).toInt()
            recyclerviewItemHomesStorageBinding.sdSizeProgress.progress = (busySize / 1024).toInt()
        }
    }

    abstract class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(position: Int)
    }
}