package com.happyworldgames.filemanager.data

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.util.Size
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.happyworldgames.filemanager.MainActivity
import com.happyworldgames.filemanager.R
import com.happyworldgames.filemanager.view.NotificationController
import kotlinx.coroutines.*
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.collections.ArrayList
import kotlin.math.roundToInt
import android.os.Environment
import java.lang.Exception


class FileUtils {

    companion object {
        private val DENSITY by lazy { MainActivity.context.resources.displayMetrics.density }
        private val ICON_SIZE_WIDTH by lazy{ (24 * DENSITY).roundToInt() } //512
        private val ICON_SIZE_HEIGHT by lazy{ (24 * DENSITY).roundToInt() } //384

        val imageExtensions = arrayOf(".png", ".jpg", ".gif", ".bmp")
        val videoExtensions = arrayOf(".mp4", ".3gp", ".mkv", ".ts", ".webm")
        val audioExtensions = arrayOf(".3gp", ".mp4", ".m4a", ".mp3", ".ogg", ".wav", ".mkv", ".amr")
        val textExtensions = arrayOf(".txt", ".xml") //and more...
        val apkExtensions = arrayOf(".apk")
        val archiveExtensions = arrayOf(".zip", ".apk") //and more...

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

        fun getDataItemFromIndex(index: Int): TabDataItem {
            return DataBase.tabsBase[index]
        }
        fun getDataItemFilesFromIndex(index: Int): TabDataItem.FileTabDataItem {
            val dataItem = getDataItemFromIndex(index)
            if(dataItem !is TabDataItem.FileTabDataItem) throw Throwable("Is not FileTabDataItem")
            return dataItem
        }

        fun getSizeAndCountFiles(file: File): Triple<Long, Long, Long> {
            return if(file.isFile) Triple(file.length(), 1, 0)
            else{
                var size: Long = 0
                var countFile: Long = 0
                var countFolder: Long = 1
                file.listFiles()!!.forEach {
                    val (one, two, three) = getSizeAndCountFiles(it)
                    size += one
                    countFile += two
                    countFolder += three
                }
                Triple(size, countFile, countFolder)
            }
        }
        fun convertLongToTime(time: Long): String {
            val date = Date(time)
            val cal = Calendar.getInstance()
            cal.time = date

            return "${cal.get(Calendar.MONTH)+1}/${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.YEAR)} ${cal.get(Calendar.HOUR)}:${cal.get(Calendar.MINUTE)}:${cal.get(Calendar.SECOND)}"
        }
        fun humanReadableByteCountBin(bytes: Long) = when {
            bytes == Long.MIN_VALUE || bytes < 0 -> "N/A"
            bytes < 1024L -> "$bytes B"
            bytes <= 0xfffccccccccccccL shr 40 -> "%.2f KiB".format(bytes.toDouble() / (0x1 shl 10))
            bytes <= 0xfffccccccccccccL shr 30 -> "%.2f MiB".format(bytes.toDouble() / (0x1 shl 20))
            bytes <= 0xfffccccccccccccL shr 20 -> "%.2f GiB".format(bytes.toDouble() / (0x1 shl 30))
            bytes <= 0xfffccccccccccccL shr 10 -> "%.2f TiB".format(bytes.toDouble() / (0x1 shl 40))
            bytes <= 0xfffccccccccccccL -> "%.2f PiB".format((bytes shr 10).toDouble() / (0x1 shl 40))
            else -> "%.2f EiB".format((bytes shr 20).toDouble() / (0x1 shl 40))
        }

        fun search(path: File, search: String, onNotify: (file: File) -> Unit) {
            if(path.isFile && path.name.contains(search)) onNotify(path)
            else if(path.isDirectory) path.listFiles()!!.forEach {
                search(it, search, onNotify)
            }
        }
        fun zip(files: List<File>, zipFile: File) {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { output ->
                files.forEach { file ->
                    zipFile(file, file.name, output)
                }
            }
        }
        private fun zipFile(fileToZip: File, fileName: String, zipOut: ZipOutputStream) {
            if(fileToZip.isDirectory) {
                zipOut.putNextEntry(ZipEntry(if(fileName.endsWith("/")) fileName else "$fileName/"))
                zipOut.closeEntry()

                fileToZip.listFiles()?.forEach { childFile ->
                    zipFile(childFile, fileName + "/" + childFile.name, zipOut)
                }
                return
            }
            FileInputStream(fileToZip).use { input ->
                BufferedInputStream(input).use { origin ->
                    val entry = ZipEntry(fileName)
                    zipOut.putNextEntry(entry)
                    origin.copyTo(zipOut, 1024)
                }
            }
        }
        fun unZip(fileZip: File, destDir: File) {
            if(!destDir.exists()) destDir.mkdirs()
            ZipInputStream(BufferedInputStream(FileInputStream(fileZip))).use { input ->
                var zipEntry = input.nextEntry
                while (zipEntry != null) {
                    val newFile = unZipNewFile(destDir, zipEntry)
                    if (zipEntry.isDirectory) {
                        if (!newFile.isDirectory && !newFile.mkdirs()) {
                            throw IOException("Failed to create directory $newFile")
                        }
                    } else {
                        // fix for Windows-created archives
                        val parent = newFile.parentFile!!
                        if (!parent.isDirectory && !parent.mkdirs()) {
                            throw IOException("Failed to create directory $parent")
                        }
                        // write file content
                        val fos = FileOutputStream(newFile)
                        var len: Int
                        val buffer = ByteArray(1024)
                        while (input.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                        fos.close()
                    }
                    zipEntry = input.nextEntry
                }
                input.closeEntry()
            }
        }
        @Throws(IOException::class)
        private fun unZipNewFile(destinationDir: File, zipEntry: ZipEntry): File {
            val destFile = File(destinationDir, zipEntry.name)
            val destDirPath = destinationDir.canonicalPath
            val destFilePath = destFile.canonicalPath
            if (!destFilePath.startsWith(destDirPath + File.separator)) {
                throw IOException("Entry is outside of the target dir: " + zipEntry.name)
            }
            return destFile
        }

        fun copy(index: Int) {
            DataBase.clipBoardBase.add(ClipBoardData(ClipBoardData.Type.COPY, this.getDataItemFilesFromIndex(index).selectedItems.toMap()))
        }
        fun cut(index: Int) {
            DataBase.clipBoardBase.add(ClipBoardData(ClipBoardData.Type.CUT, this.getDataItemFilesFromIndex(index).selectedItems.toMap()))
        }
        suspend fun paste(context: Context, currentPage: Int, index: Int, requestOverWrite: (file: File, requestWrite: (i: Int) -> Unit) -> Unit) {
            paste(context, currentPage, DataBase.clipBoardBase[index], requestOverWrite)
        }
        private suspend fun paste(context: Context, currentPage: Int, clipBoardData: ClipBoardData, requestOverWrite: (file: File, requestWrite: (i: Int) -> Unit) -> Unit) {
            val type = clipBoardData.type
            val files = clipBoardData.files

            var request = true
            var overWrite = false
            val onUpdateNotify = NotificationController(context).createNotifyFile(clipBoardData)

            var breakFor = false
            var waitRequest = false
            files.values.forEachIndexed pasteFor@{ progress, it ->
                onUpdateNotify(progress)
                val fileTo = File(this.getDataItemFilesFromIndex(currentPage).path, it.name)
                //need stop requestOverWrite when his show, and start when hide
                if(fileTo.exists() && request){
                    waitRequest = true
                    requestOverWrite(fileTo) { result ->
                        overWrite = when (result) {
                            1 -> true
                            2 -> { request = false; false }
                            3 -> { request = false; true }
                            4 -> { breakFor = true; false }
                            else -> false
                        }
                        try {
                            it.copyTo(fileTo, overWrite)
                            if (overWrite && type == ClipBoardData.Type.CUT) it.delete()
                        } catch (e: Throwable) { e.printStackTrace() }
                        waitRequest = false
                    }
                }else try {
                    it.copyTo(fileTo, overWrite)
                    if(overWrite && type == ClipBoardData.Type.CUT) it.delete()
                }catch (e: Throwable){ e.printStackTrace() }
                while (waitRequest) delay(200)
                if(breakFor) return@pasteFor
            }
            if(type == ClipBoardData.Type.CUT) DataBase.clipBoardBase.remove(clipBoardData)
            withContext (Dispatchers.Main) {
                (MainActivity.context as MainActivity).refreshCurrentItem()
            }
        }
        fun delete(index: Int) {
            this.getDataItemFilesFromIndex(index).selectedItems.values.forEach {
                delete(it)
            }
        }
        private fun delete(file: File) {
            if(file.isDirectory) file.listFiles()!!.forEach {
                delete(it)
            }
            file.delete()
        }
        fun rename(file: File, newName: String) {
            val newFile = File(file.parentFile?.absolutePath, newName)
            file.renameTo(newFile)
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
            return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ThumbnailUtils.createImageThumbnail(file, Size(ICON_SIZE_WIDTH, ICON_SIZE_HEIGHT), null)
                else Bitmap.createScaledBitmap(BitmapFactory.decodeFile(file.absolutePath), ICON_SIZE_WIDTH, ICON_SIZE_HEIGHT, false)
        }
        fun createVideoThumbnailUtils(file: File): Bitmap {
            return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ThumbnailUtils.createVideoThumbnail(file, Size(ICON_SIZE_WIDTH, ICON_SIZE_HEIGHT), null)
                else createVideoThumbnail(file.absolutePath) ?: throw Throwable("Image null")
        }
        private fun createVideoThumbnail(filePath: String?): Bitmap? {
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

            Bitmap.createScaledBitmap(bitmap, ICON_SIZE_WIDTH, ICON_SIZE_HEIGHT, true)

            return bitmap
        }
        fun createAudioThumbnailUtils(file: File): Bitmap {
            return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ThumbnailUtils.createAudioThumbnail(file, Size(ICON_SIZE_WIDTH, ICON_SIZE_HEIGHT), null)
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

        fun showChoiceFileManager(context: Context, pathF: File, onSelect: (path: String) -> Unit) {
            val path = if(pathF.isFile) pathF.parentFile else pathF

            val builder = AlertDialog.Builder(context)
            builder.setTitle("Choice")
            val names = arrayListOf<String>()
            sort(path.listFiles()!!).forEach {
                names.add(it.name)
            }
            builder.setItems(names.toTypedArray()) { _, i ->
                val file = File(path, names[i])
                if (file.isDirectory) showChoiceFileManager(context, file, onSelect)
                else onSelect(file.parentFile!!.absolutePath)
            }
            builder.setPositiveButton("Select") { _, _ ->
                onSelect(path.absolutePath)
            }
            builder.setNegativeButton("Cancel"){_,_->}
            builder.setNeutralButton("Go Up"){_, _ ->
                val file = path.parentFile!!
                showChoiceFileManager(context, if(file.canRead()) file else path, onSelect)
            }
            builder.show()
        }
    }

    class ExternalStorage {
        companion object {
            const val SD_CARD = "sdCard"
            const val EXTERNAL_SD_CARD = "externalSdCard"

            /**
             * @return True if the external storage is available. False otherwise.
             */
            fun isAvailable(): Boolean {
                val state = Environment.getExternalStorageState()
                return if (Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state) {
                    true
                } else false
            }

            fun getSdCardPath(): String? {
                return Environment.getExternalStorageDirectory().path + "/"
            }

            /**
             * @return True if the external storage is writable. False otherwise.
             */
            fun isWritable(): Boolean {
                val state = Environment.getExternalStorageState()
                return if (Environment.MEDIA_MOUNTED == state) {
                    true
                } else false
            }

            /**
             * @return A map of all storage locations available
             */
            fun getAllStorageLocations(): Map<String, File>? {
                val map: MutableMap<String, File> = HashMap(10)
                val mMounts: MutableList<String> = ArrayList(10)
                val mVold: MutableList<String> = ArrayList(10)
                mMounts.add("/mnt/sdcard")
                mVold.add("/mnt/sdcard")
                try {
                    val mountFile = File("/proc/mounts")
                    if (mountFile.exists()) {
                        val scanner = Scanner(mountFile)
                        while (scanner.hasNext()) {
                            val line = scanner.nextLine()
                            if (line.startsWith("/dev/block/vold/")) {
                                val lineElements = line.split(" ").toTypedArray()
                                val element = lineElements[1]

                                // don't add the default mount path
                                // it's already in the list.
                                if (element != "/mnt/sdcard") mMounts.add(element)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    val voldFile = File("/system/etc/vold.fstab")
                    if (voldFile.exists()) {
                        val scanner = Scanner(voldFile)
                        while (scanner.hasNext()) {
                            val line = scanner.nextLine()
                            if (line.startsWith("dev_mount")) {
                                val lineElements = line.split(" ").toTypedArray()
                                var element = lineElements[2]
                                if (element.contains(":")) element =
                                    element.substring(0, element.indexOf(":"))
                                if (element != "/mnt/sdcard") mVold.add(element)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                var i = 0
                while (i < mMounts.size) {
                    val mount = mMounts[i]
                    if (!mVold.contains(mount)) mMounts.removeAt(i--)
                    i++
                }
                mVold.clear()
                val mountHash: MutableList<String> = ArrayList(10)
                for (mount in mMounts) {
                    val root = File(mount)
                    if (root.exists() && root.isDirectory && root.canWrite()) {
                        val list = root.listFiles()
                        var hash = "["
                        if (list != null) {
                            for (f in list) {
                                hash += f.name.hashCode().toString() + ":" + f.length() + ", "
                            }
                        }
                        hash += "]"
                        if (!mountHash.contains(hash)) {
                            var key = SD_CARD + "_" + map.size
                            if (map.size == 0) {
                                key = SD_CARD
                            } else if (map.size == 1) {
                                key = EXTERNAL_SD_CARD
                            }
                            mountHash.add(hash)
                            map[key] = root
                        }
                    }
                }
                mMounts.clear()
                if (map.isEmpty()) {
                    map[SD_CARD] = Environment.getExternalStorageDirectory()
                }
                return map
            }
        }
    }
}