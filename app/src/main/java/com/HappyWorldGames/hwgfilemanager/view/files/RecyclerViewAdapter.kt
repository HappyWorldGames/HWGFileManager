package com.happyworldgames.hwgfilemanager.view.files

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.happyworldgames.hwgfilemanager.R
import com.happyworldgames.hwgfilemanager.data.FileUtils
import com.happyworldgames.hwgfilemanager.data.TabsDataBase
import com.happyworldgames.hwgfilemanager.databinding.RecyclerviewItemFilesLargeIconBinding
import kotlinx.coroutines.*
import java.io.File

class RecyclerViewAdapter(backButton: Button, var tabPosition: Int = 0) : RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder>(){

    private lateinit var parentView: View
    init {
        backButton.setOnClickListener {
            goTo(getFilePath().parentFile!!)
        }
    }

    private fun getFilePath(): File = File(TabsDataBase.dataBase[tabPosition].path)
    /*
        Function for open file or directory
     */
    fun goTo(path: File) {
        if(!path.exists()) return
        if(path.isDirectory) {
            if (path.canRead()) TabsDataBase.dataBase[tabPosition].path = path.absolutePath
            else Snackbar.make(parentView, "Can`t read.", Snackbar.LENGTH_SHORT).show()
        }else if(path.isFile) {
            Log.e("LOG", "Mime Type: " + FileUtils.getMimeType(path.absolutePath))
            val intent = Intent().setDataAndType(Uri.fromFile(path), FileUtils.getMimeType(path.absolutePath)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            parentView.context.startActivity(Intent.createChooser(intent, "Select a file"))
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        parentView = parent
        val itemView = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return IconViewHolder(itemView)
    }

    /*
        *Need Replace
     */
    override fun getItemViewType(position: Int): Int {
        return R.layout.recyclerview_item_files_large_icon
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = if(getFilePath().exists()) getFilePath().listFiles()!!.size else 0

    inner class IconViewHolder(itemView: View) : MyViewHolder(itemView){
        private val recyclerviewItemFilesLargeIconBinding: RecyclerviewItemFilesLargeIconBinding = RecyclerviewItemFilesLargeIconBinding.bind(itemView)
        private lateinit var fileInfo: File

        init {
            recyclerviewItemFilesLargeIconBinding.select.visibility = View.GONE
            //recyclerviewItemFilesLargeIconBinding.select.visibility = View.VISIBLE
            recyclerviewItemFilesLargeIconBinding.root.setOnClickListener {
                goTo(fileInfo)
            }
        }

        override fun bind(position: Int) {
            fileInfo = FileUtils.sort(getFilePath().listFiles()!!)[position]

            recyclerviewItemFilesLargeIconBinding.icon.setImageResource(if(fileInfo.isFile) R.drawable.file else R.drawable.folder)
            GlobalScope.launch(Dispatchers.Main) {
                if(FileUtils.checkIfFileHasExtension(fileInfo.name, FileUtils.imageExtensions)) recyclerviewItemFilesLargeIconBinding.icon.setImageDrawable(Drawable.createFromPath(fileInfo.absolutePath))
                else if(FileUtils.checkIfFileHasExtension(fileInfo.name, FileUtils.videoExtensions)) recyclerviewItemFilesLargeIconBinding.icon.setImageBitmap(FileUtils.createTrumbnailUtils(fileInfo))
            }

            recyclerviewItemFilesLargeIconBinding.name.text = if(fileInfo.name.length > 18) fileInfo.name.subSequence(0, 18) else fileInfo.name
        }
    }

    abstract class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(position: Int)
    }
}