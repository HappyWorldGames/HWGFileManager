package com.happyworldgames.filemanager.view.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.happyworldgames.filemanager.R
import com.happyworldgames.filemanager.databinding.FileViewItemBinding
import com.happyworldgames.filemanager.view.tabs.FilesTabData
import java.io.File

class FilesViewAdapter(private val filesTabData: FilesTabData, private val backButton: Button) : RecyclerView.Adapter<FilesViewAdapter.FilesViewBaseViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilesViewBaseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.file_view_item, parent, false)
        return FilesViewBaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilesViewBaseViewHolder, position: Int) {
        val file = File(filesTabData.path).listFiles()?.sorted()?.get(position)?:return

        holder.fileViewItemBinding.fileName.text = file.name
        holder.fileViewItemBinding.fileIcon.setImageResource(if (file.isFile) R.drawable.file else R.drawable.folder)
        if (file.name[0] == '.') holder.fileViewItemBinding.fileIcon.alpha = 0.5f

        holder.fileViewItemBinding.root.setOnClickListener {
            openFile(file)
        }
        backButton.setOnClickListener {
            val parentFile = File(filesTabData.path).parentFile?:return@setOnClickListener
            openFile(parentFile)
        }
    }

    override fun getItemCount(): Int = File(filesTabData.path).listFiles()?.size?:0

    private fun openFile(file: File) {
        if (file.isDirectory) {
            notifyItemRangeRemoved(0, itemCount)
            filesTabData.path = file.absolutePath
            backButton.text = filesTabData.path
            notifyItemRangeInserted(0, itemCount)
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri: Uri = Uri.fromFile(file)
            intent.setDataAndType(uri, "*/" + file.extension)
            backButton.context.startActivity(intent)
        }
    }

    class FilesViewBaseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileViewItemBinding = FileViewItemBinding.bind(view)
    }
}