package com.happyworldgames.filemanager.view.viewpager

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.happyworldgames.filemanager.data.DataBase
import com.happyworldgames.filemanager.data.FileUtils
import com.happyworldgames.filemanager.data.TabDataItem
import com.happyworldgames.filemanager.databinding.ViewPagerFilesItemBinding
import com.happyworldgames.filemanager.view.files.FilesRecyclerViewAdapter

class PagerAdapter(private val context: Context, private val pathButton: Button, val onSwitchSelectModeListener: FilesRecyclerViewAdapter.SwitchSelectModeListener): RecyclerView.Adapter<PagerAdapter.MyViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder  =
        FilesPageHolder(LayoutInflater.from(context).inflate(viewType, parent, false))

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemViewType(position: Int): Int = DataBase.tabsBase[position].type

    override fun getItemCount(): Int = DataBase.tabsBase.size

    inner class FilesPageHolder(itemView: View): MyViewHolder(itemView){
        val viewPagerFilesItemBinding: ViewPagerFilesItemBinding = ViewPagerFilesItemBinding.bind(itemView)

        init {
            viewPagerFilesItemBinding.filesList.adapter = FilesRecyclerViewAdapter(pathButton, onSwitchSelectMode = onSwitchSelectModeListener)
        }

        override fun bind(position: Int) {
            (viewPagerFilesItemBinding.filesList.adapter as FilesRecyclerViewAdapter).tabPosition = position
            viewPagerFilesItemBinding.filesList.layoutManager = if(FileUtils.getDataItemFromIndex(position).viewType == TabDataItem.FileTabDataItem.ViewType.Linear) LinearLayoutManager(context) else GridLayoutManager(context, 4)
            viewPagerFilesItemBinding.filesList.adapter?.notifyDataSetChanged()
        }
    }

    abstract class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(position: Int)
    }
}