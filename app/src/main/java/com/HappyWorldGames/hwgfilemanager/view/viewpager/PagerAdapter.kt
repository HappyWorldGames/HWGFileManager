package com.happyworldgames.hwgfilemanager.view.viewpager

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.happyworldgames.hwgfilemanager.data.TabsDataBase
import com.happyworldgames.hwgfilemanager.databinding.ViewPagerItemBinding

class PagerAdapter(private val context: Context): RecyclerView.Adapter<PagerAdapter.MyViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder  =
        PageHolder(LayoutInflater.from(context).inflate(viewType, parent, false))

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemViewType(position: Int): Int = TabsDataBase.dataBase[position].type

    override fun getItemCount(): Int = TabsDataBase.dataBase.size

    inner class PageHolder(itemView: View): MyViewHolder(itemView){
        private val viewPagerItemBinding: ViewPagerItemBinding = ViewPagerItemBinding.bind(itemView)

        init {
            viewPagerItemBinding.filesList.layoutManager = GridLayoutManager(context, 4)
            viewPagerItemBinding.filesList.adapter = com.happyworldgames.hwgfilemanager.view.files.RecyclerViewAdapter(viewPagerItemBinding.path)
        }

        override fun bind(position: Int) {
            viewPagerItemBinding.path.text = TabsDataBase.dataBase[position].path
            (viewPagerItemBinding.filesList.adapter as com.happyworldgames.hwgfilemanager.view.files.RecyclerViewAdapter).tabPosition = position
            viewPagerItemBinding.filesList.adapter?.notifyDataSetChanged()
        }
    }

    abstract class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(position: Int)
    }
}