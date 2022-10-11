package com.happyworldgames.filemanager.view.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.happyworldgames.filemanager.R
import com.happyworldgames.filemanager.databinding.StorageItemViewBinding
import com.happyworldgames.filemanager.view.tabs.FilesTabData
import java.io.File

class StorageViewAdapter(private val context: Context, private val viewPagerAdapter: ViewPagerAdapter) : RecyclerView.Adapter<StorageViewAdapter.BaseStorageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseStorageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.storage_item_view, parent, false)
        return BaseStorageViewHolder(view)
    }

    @SuppressLint("UsableSpace")
    override fun onBindViewHolder(holder: BaseStorageViewHolder, position: Int) {
        val tempText = "/Android/data/" + context.packageName + "/files"
        var localExternalFileDir = context.getExternalFilesDirs(null)[position]
        if (localExternalFileDir.absolutePath.contains(tempText)) localExternalFileDir = File(localExternalFileDir.absolutePath.replace(tempText, ""))

        val usableSpace = localExternalFileDir.usableSpace
        val occupiedSize = localExternalFileDir.totalSpace - usableSpace
        val occupiedSizeText = "${humanReadableByteCountBin(occupiedSize)}/${humanReadableByteCountBin(localExternalFileDir.totalSpace)}"

        holder.storageName.text = localExternalFileDir.name
        holder.storageSize.text = occupiedSizeText
        holder.storageOccupied.progress = (occupiedSize * 100 / localExternalFileDir.totalSpace).toInt()

        holder.rootView.setOnClickListener {
            viewPagerAdapter.addTab(FilesTabData(localExternalFileDir.absolutePath))
        }
    }

    override fun getItemCount(): Int = context.getExternalFilesDirs(null).size

    private fun humanReadableByteCountBin(bytes: Long) = when {
        bytes == Long.MIN_VALUE || bytes < 0 -> "N/A"
        bytes < 1024L -> "$bytes B"
        bytes <= 0xfffccccccccccccL shr 40 -> "%.1f KiB".format(bytes.toDouble() / (0x1 shl 10))
        bytes <= 0xfffccccccccccccL shr 30 -> "%.1f MiB".format(bytes.toDouble() / (0x1 shl 20))
        bytes <= 0xfffccccccccccccL shr 20 -> "%.1f GiB".format(bytes.toDouble() / (0x1 shl 30))
        bytes <= 0xfffccccccccccccL shr 10 -> "%.1f TiB".format(bytes.toDouble() / (0x1 shl 40))
        bytes <= 0xfffccccccccccccL -> "%.1f PiB".format((bytes shr 10).toDouble() / (0x1 shl 40))
        else -> "%.1f EiB".format((bytes shr 20).toDouble() / (0x1 shl 40))
    }

    class BaseStorageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val storageItemViewBinding = StorageItemViewBinding.bind(view)

        val rootView = storageItemViewBinding.root
        val storageName = storageItemViewBinding.storageName
        val storageSize = storageItemViewBinding.storageSize
        val storageOccupied = storageItemViewBinding.storageOccupied
    }
}