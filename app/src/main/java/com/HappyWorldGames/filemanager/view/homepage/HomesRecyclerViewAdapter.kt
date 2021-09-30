package com.happyworldgames.filemanager.view.homepage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.happyworldgames.filemanager.R
import com.happyworldgames.filemanager.data.DataBase
import com.happyworldgames.filemanager.data.TabDataItem
import com.happyworldgames.filemanager.databinding.ActivityMainBinding
import com.happyworldgames.filemanager.databinding.RecyclerviewItemHomesStorageBinding
import com.happyworldgames.filemanager.view.viewpager.PagerAdapter

class HomesRecyclerViewAdapter(val pagerAdapter: PagerAdapter) : RecyclerView.Adapter<HomesRecyclerViewAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_item_homes_storage, parent, false)
        return StorageViewHolder(itemView, pagerAdapter)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = 2

    inner class StorageViewHolder(itemView: View, pagerAdapter: PagerAdapter) : MyViewHolder(itemView) {
        private val recyclerviewItemHomesStorageBinding = RecyclerviewItemHomesStorageBinding.bind(itemView)

        init {
            recyclerviewItemHomesStorageBinding.root.setOnClickListener {
                DataBase.tabsBase.add(TabDataItem.FileTabDataItem(R.layout.view_pager_files_item, "/sdcard", TabDataItem.FileTabDataItem.ViewType.Grid))
                val pos = DataBase.tabsBase.size - 1
                pagerAdapter.notifyItemInserted(pos)
                pagerAdapter.activityMainBinding.pager.currentItem = pos
            }
        }

        override fun bind(position: Int) {
            recyclerviewItemHomesStorageBinding.sdName.text = "Sd $position"
            recyclerviewItemHomesStorageBinding.sdSizeProgress.progress = 35
            recyclerviewItemHomesStorageBinding.sdSize.text = "35/60 GB"
        }
    }

    abstract class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(position: Int)
    }
}