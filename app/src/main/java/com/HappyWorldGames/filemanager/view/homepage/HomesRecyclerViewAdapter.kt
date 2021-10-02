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

class HomesRecyclerViewAdapter(private val pagerAdapter: PagerAdapter) : RecyclerView.Adapter<HomesRecyclerViewAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_item_homes_storage, parent, false)
        return StorageViewHolder(itemView, pagerAdapter)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = FileUtils.ExternalStorage.getAllStorageLocations()!!.size //if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) 2 else 1

    inner class StorageViewHolder(itemView: View, pagerAdapter: PagerAdapter) : MyViewHolder(itemView) {
        private val recyclerviewItemHomesStorageBinding = RecyclerviewItemHomesStorageBinding.bind(itemView)

        override fun bind(position: Int) {
            val dataItem = FileUtils.ExternalStorage.getAllStorageLocations()?.values!!.toList()

            recyclerviewItemHomesStorageBinding.root.setOnClickListener {
                DataBase.tabsBase.add(TabDataItem.FileTabDataItem(R.layout.view_pager_files_item, dataItem[position].absolutePath, TabDataItem.FileTabDataItem.ViewType.Grid))
                val pos = DataBase.tabsBase.size - 1
                pagerAdapter.notifyItemInserted(pos)
                pagerAdapter.activityMainBinding.pager.currentItem = pos
            }
            recyclerviewItemHomesStorageBinding.sdName.text = dataItem[position].name /*when(position){
                0 -> "Phone"
                else -> "Memory Card"
            }*/
            val statFs = StatFs(dataItem[position].absolutePath) /*when(position){
                0 -> StatFs(itemView.context.getExternalFilesDir(null)?.absolutePath)
                else -> StatFs(itemView.context.getExternalFilesDir(Environment.MEDIA_MOUNTED)?.absolutePath)
            }*/
            val txt = "${FileUtils.humanReadableByteCountBin(statFs.availableBytes)} / ${FileUtils.humanReadableByteCountBin(statFs.totalBytes)}"
            recyclerviewItemHomesStorageBinding.sdSize.text = txt
            recyclerviewItemHomesStorageBinding.sdSizeProgress.max = statFs.totalBytes.toInt()
            recyclerviewItemHomesStorageBinding.sdSizeProgress.progress = statFs.availableBytes.toInt()
        }
    }

    abstract class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(position: Int)
    }
}