package com.happyworldgames.hwgfilemanager.view.files

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.happyworldgames.hwgfilemanager.R
import com.happyworldgames.hwgfilemanager.data.FileUtils
import com.happyworldgames.hwgfilemanager.data.DataBase
import com.happyworldgames.hwgfilemanager.data.TabDataItem
import com.happyworldgames.hwgfilemanager.databinding.RecyclerviewItemFilesLargeIconBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.CoroutineContext

class FilesRecyclerViewAdapter(private val backButton: Button, var tabPosition: Int = 0, private val onSwitchSelectMode: SwitchSelectModeListener) : RecyclerView.Adapter<FilesRecyclerViewAdapter.MyViewHolder>(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()

    private lateinit var parentView: View
    private lateinit var context: Context
    init {
        backButton.setOnClickListener {
            if(!isSelectMode()) goTo(getFilePath().parentFile!!)
            else switchSelectMode(false)
        }
    }

    fun isSelectMode(): Boolean = getDataItem().selectMode
    private fun getFilePath(): File = File(getDataItem().path)
    private fun getDataItem() : TabDataItem.FileTabDataItem = FileUtils.getDataItemFromIndex(tabPosition)
    /*
        Function for open file or directory
    */
    fun goTo(path: File, goBack: Boolean = false) {
        if(!path.exists()) return
        if(path.isDirectory) {
            if (path.canRead()) {
                getDataItem().path = path.absolutePath
                backButton.text = (path.parentFile!!.name + "/" + path.name)
                notifyDataSetChanged()
                if(!goBack) getDataItem().history.add(path.absolutePath)
            }else Snackbar.make(parentView, "Can`t read.", Snackbar.LENGTH_SHORT).show()
        }else if(path.isFile) {
            val intent = Intent().setDataAndType(FileUtils.getUriFromFile(parentView.context, path), FileUtils.getMimeType(path.absolutePath)).setAction(Intent.ACTION_VIEW).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            parentView.context.startActivity(Intent.createChooser(intent, "Select a file"))
        }
        switchSelectMode(false)
    }
    fun switchSelectMode(turnOn: Boolean) {
        if(isSelectMode() == turnOn) return
        getDataItem().selectMode = turnOn
        if(turnOn) getDataItem().selectedItems.clear()
        onSwitchSelectMode.onSwitch(turnOn)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        parentView = parent
        context = parentView.context
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
            recyclerviewItemFilesLargeIconBinding.root.setOnClickListener {
                if(!isSelectMode()) goTo(fileInfo)
                else{
                    val dataItem = getDataItem()
                    if(dataItem.selectedItems.containsKey(fileInfo.absolutePath)) dataItem.selectedItems.remove(fileInfo.absolutePath)
                    else dataItem.selectedItems[fileInfo.absolutePath] = fileInfo
                    recyclerviewItemFilesLargeIconBinding.select.isChecked = dataItem.selectedItems.containsKey(fileInfo.absolutePath)

                    onSwitchSelectMode.onEnableOrDisableBottomAppBar(dataItem.selectedItems.isNotEmpty())
                }
            }
            recyclerviewItemFilesLargeIconBinding.root.setOnLongClickListener {
                switchSelectMode(true)

                val dataItem = getDataItem()
                dataItem.selectedItems[fileInfo.absolutePath] = fileInfo
                recyclerviewItemFilesLargeIconBinding.select.isChecked = dataItem.selectedItems.containsKey(fileInfo.absolutePath)

                true
            }
        }

        override fun bind(position: Int) {
            if(isSelectMode()) recyclerviewItemFilesLargeIconBinding.select.visibility = View.VISIBLE
            else recyclerviewItemFilesLargeIconBinding.select.visibility = View.GONE
            launch(Dispatchers.IO) {
                fileInfo = FileUtils.sort(getFilePath().listFiles()!!)[position]

                if(fileInfo.name.startsWith(".")) recyclerviewItemFilesLargeIconBinding.icon.alpha = 0.5f
                else recyclerviewItemFilesLargeIconBinding.icon.alpha = 1f

                val fileName = if(fileInfo.name.length > 18) fileInfo.name.subSequence(0, 18) else fileInfo.name
                launch(Dispatchers.Main) { recyclerviewItemFilesLargeIconBinding.name.text = fileName }

                val defaultFileIcon = if(fileInfo.isFile) R.drawable.file else R.drawable.folder
                launch(Dispatchers.Main) { recyclerviewItemFilesLargeIconBinding.icon.setImageResource(defaultFileIcon) }

                val image = when {
                    FileUtils.checkIfFileHasExtension(fileInfo.name, FileUtils.imageExtensions) -> try{ FileUtils.createImageThumbnailUtils(fileInfo) }catch (e: Throwable){ FileUtils.drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.broken_image)) }
                    FileUtils.checkIfFileHasExtension(fileInfo.name, FileUtils.videoExtensions) -> try{ FileUtils.createVideoThumbnailUtils(fileInfo)}catch (e: Throwable){ FileUtils.drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.broken_video)) }
                    FileUtils.checkIfFileHasExtension(fileInfo.name, FileUtils.audioExtensions) -> try{ FileUtils.createAudioThumbnailUtils(fileInfo) }catch(e: Throwable){ FileUtils.drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.audio)) }
                    FileUtils.checkIfFileHasExtension(fileInfo.name, FileUtils.textExtensions) -> FileUtils.createTextThumbnailUtils(parentView.context)
                    FileUtils.checkIfFileHasExtension(fileInfo.name, FileUtils.apkExtensions) -> try{ FileUtils.createApkThumbnailUtils(parentView.context, fileInfo) }catch(e: Throwable){ FileUtils.drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.apk)) }
                    else -> null
                }
                if(image != null) launch(Dispatchers.Main){ recyclerviewItemFilesLargeIconBinding.icon.setImageBitmap(image) }

                launch(Dispatchers.Main){ recyclerviewItemFilesLargeIconBinding.select.isChecked = getDataItem().selectedItems.containsKey(fileInfo.absolutePath) }
            }
        }
    }

    abstract class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(position: Int)
    }

    abstract class SwitchSelectModeListener {
        abstract fun onSwitch(switched: Boolean)
        abstract fun onEnableOrDisableBottomAppBar(enable: Boolean)
    }
}