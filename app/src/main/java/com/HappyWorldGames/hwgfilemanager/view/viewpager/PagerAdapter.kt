package com.happyworldgames.hwgfilemanager.view.viewpager

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.happyworldgames.hwgfilemanager.data.DataBase
import com.happyworldgames.hwgfilemanager.data.TabDataItem
import com.happyworldgames.hwgfilemanager.databinding.ViewPagerFilesItemBinding
import com.happyworldgames.hwgfilemanager.view.files.FilesRecyclerViewAdapter
import java.io.File

class PagerAdapter(private val context: Context, val onSwitchSelectModeListener: FilesRecyclerViewAdapter.SwitchSelectModeListener): RecyclerView.Adapter<PagerAdapter.MyViewHolder>(){

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
            viewPagerFilesItemBinding.filesList.layoutManager = GridLayoutManager(context, 4)
            viewPagerFilesItemBinding.filesList.adapter = FilesRecyclerViewAdapter(viewPagerFilesItemBinding.path, onSwitchSelectMode = onSwitchSelectModeListener)
        }

        override fun bind(position: Int) {
            val path = File((DataBase.tabsBase[position] as TabDataItem.FileTabDataItem).path)
            viewPagerFilesItemBinding.path.text = (path.parentFile!!.name + "/" + path.name)

            (viewPagerFilesItemBinding.filesList.adapter as FilesRecyclerViewAdapter).tabPosition = position
            viewPagerFilesItemBinding.filesList.adapter?.notifyDataSetChanged()
        }
    }

    abstract class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(position: Int)
    }
}