package com.happyworldgames.hwgfilemanager.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.media.ThumbnailUtils.OPTIONS_RECYCLE_INPUT
import android.media.ThumbnailUtils.extractThumbnail
import android.net.Uri
import android.os.Build
import android.util.Size
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.happyworldgames.hwgfilemanager.R
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class FileUtils {

    companion object {
        private const val TARGET_SIZE_MICRO_THUMBNAIL = 96
        private const val MINI_KIND = 1
        private const val MICRO_KIND = 3

        private const val SIZE_MINI_KIND_WIDTH = 512
        private const val SIZE_MINI_KIND_HEIGHT = 384

        val imageExtensions = arrayOf(".png", ".jpg", ".gif", ".bmp")
        val videoExtensions = arrayOf(".mp4", ".3gp", ".mkv", ".ts", ".webm")
        val audioExtensions = arrayOf(".3gp", ".mp4", ".m4a", ".mp3", ".ogg", ".wav", ".mkv", ".amr")
        val textExtensions = arrayOf(".txt", ".xml") //and more...
        val apkExtensions = arrayOf(".apk")

        fun sort(array: Array<File>) : ArrayList<File>{
            array.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER, { it.name }))

            val files = arrayListOf<File>()
            val folder = arrayListOf<File>()
            for(item in array)
                if(item.isFile) files.add(item)
                else if(item.isDirectory) folder.add(item)

            val sorted = arrayListOf<File>()
            sorted.addAll(folder)
            sorted.addAll(files)

            return sorted
        }

        fun getDataItemFromIndex(index: Int): TabDataItem.FileTabDataItem {
            val dataItem = DataBase.tabsBase[index]
            if(dataItem !is TabDataItem.FileTabDataItem) throw Throwable("Is not FileTabDataItem")
            return dataItem
        }

        fun getSizeFile(file: File): Long {
            return if(file.isFile) file.length()
            else{
                var size: Long = 0
                file.listFiles()!!.forEach {
                    size += getSizeFile(it)
                }
                size
            }
        }
        fun convertLongToTime(time: Long): String {
            val date = Date(time)
            val cal = Calendar.getInstance()
            cal.time = date

            return "${cal.get(Calendar.MONTH)+1}/${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.YEAR)} ${cal.get(Calendar.HOUR)}:${cal.get(Calendar.MINUTE)}:${cal.get(Calendar.SECOND)}"
        }

        fun search(path: File, search: String, onNotify: (file: File) -> Unit) {
            if(path.isFile && path.name.contains(search)) onNotify(path)
            else if(path.isDirectory) path.listFiles()!!.forEach {
                search(it, search, onNotify)
            }
        }

        fun copy(index: Int) {
            DataBase.clipBoardBase.add(ClipBoardData(ClipBoardData.Type.COPY, getDataItemFromIndex(index).selectedItems.toMap()))
        }
        fun cut(index: Int) {
            DataBase.clipBoardBase.add(ClipBoardData(ClipBoardData.Type.CUT, getDataItemFromIndex(index).selectedItems.toMap()))
        }
        fun paste(currentPage: Int, index: Int) {
            val type = DataBase.clipBoardBase[index].type
            val files = DataBase.clipBoardBase[index].files

            files.values.forEach {
                val fileTo = File(getDataItemFromIndex(currentPage).path, it.name)
                it.copyTo(fileTo)
                if(type == ClipBoardData.Type.CUT) it.delete()
            }
        }
        fun delete(index: Int) {
            getDataItemFromIndex(index).selectedItems.values.forEach {
                delete(it)
            }
        }
        private fun delete(file: File) {
            if(file.isDirectory) file.listFiles()!!.forEach {
                delete(it)
            }
            file.delete()
        }

        fun shareMultiple(context: Context, files: List<File>) {
            val multi = files.size > 1 || files[0].isDirectory
            val intent = Intent(if(multi) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND)
            if(multi) {
                val uris: ArrayList<Uri> = ArrayList()
                for (file in files) {
                    addShareMultipleList(context, file, uris)
                }
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }else intent.data = getUriFromFile(context, files[0])
            intent.type = "*/*"
            context.startActivity(Intent.createChooser(intent, "Share:"))
        }
        private fun addShareMultipleList(context: Context, file: File, uris: ArrayList<Uri>) {
            if(uris.size > 100) throw Throwable("Many items (>100)")
            if(file.isDirectory) file.listFiles()!!.forEach {
                addShareMultipleList(context, it, uris)
            }
            else uris.add(getUriFromFile(context, file))
        }

        fun checkIfFileHasExtension(name: String, extensions: Array<String>): Boolean {
            val check = name.lowercase()
            extensions.forEach { extension -> if(check.endsWith(extension)) return true }
            return false
        }

        fun createImageThumbnailUtils(file: File): Bitmap {
            return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ThumbnailUtils.createImageThumbnail(file, Size(SIZE_MINI_KIND_WIDTH, SIZE_MINI_KIND_HEIGHT), null)
                else Bitmap.createScaledBitmap(BitmapFactory.decodeFile(file.absolutePath), SIZE_MINI_KIND_WIDTH, SIZE_MINI_KIND_HEIGHT, false)
        }
        fun createVideoThumbnailUtils(file: File): Bitmap {
            return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ThumbnailUtils.createVideoThumbnail(file, Size(SIZE_MINI_KIND_WIDTH, SIZE_MINI_KIND_HEIGHT), null)
                else createVideoThumbnail(file.absolutePath, MINI_KIND) ?: throw Throwable("Image null")
        }
        private fun createVideoThumbnail(filePath: String?, kind: Int): Bitmap? {
            var bitmap: Bitmap? = null
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(filePath)
                bitmap = retriever.getFrameAtTime(-1)
            } catch (ex: IllegalArgumentException) {
                // Assume this is a corrupt video file
            } catch (ex: RuntimeException) {
                // Assume this is a corrupt video file.
            } finally {
                try {
                    retriever.release()
                } catch (ex: RuntimeException) {
                    // Ignore failures while cleaning up.
                }
            }
            if (bitmap == null) return null
            if (kind == MINI_KIND) {
                // Scale down the bitmap if it's too large.
                val width = bitmap.width
                val height = bitmap.height
                val max = width.coerceAtLeast(height)
                if (max > 512) {
                    val scale = 512f / max
                    val w = (scale * width).roundToInt()
                    val h = (scale * height).roundToInt()
                    bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true)
                }
            } else if (kind == MICRO_KIND) {
                bitmap = extractThumbnail(
                    bitmap,
                    TARGET_SIZE_MICRO_THUMBNAIL,
                    TARGET_SIZE_MICRO_THUMBNAIL,
                    OPTIONS_RECYCLE_INPUT
                )
            }
            return bitmap
        }
        fun createAudioThumbnailUtils(file: File): Bitmap {
            return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ThumbnailUtils.createAudioThumbnail(file, Size(SIZE_MINI_KIND_WIDTH, SIZE_MINI_KIND_HEIGHT), null)
                else throw Throwable("Use default icon")
        }
        fun createTextThumbnailUtils(context: Context): Bitmap = drawableToBitmap(ContextCompat.getDrawable(context, R.drawable.file_text)) ?: throw Throwable("He is null!!!")
        fun createApkThumbnailUtils(context: Context, file: File): Bitmap? {
            val applicationInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)?.applicationInfo
            applicationInfo?.sourceDir = file.absolutePath
            applicationInfo?.publicSourceDir = file.absolutePath
            return if(applicationInfo != null) drawableToBitmap(context.packageManager.getApplicationIcon(applicationInfo)) else throw Throwable("Use standard icon")
        }

        fun drawableToBitmap(drawable: Drawable?): Bitmap? {
            if(drawable == null) return null
            if (drawable is BitmapDrawable) {
                if (drawable.bitmap != null) {
                    return drawable.bitmap
                }
            }
            val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0)
                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
                else Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }

        fun getMimeType(url: String?): String? {
            var type: String? = null
            val extension = MimeTypeMap.getFileExtensionFromUrl(url)
            if (extension != null) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }
            return type
        }

        fun getUriFromFile(context: Context, file: File): Uri {
            return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", file)
            else Uri.fromFile(file)
        }
    }

}