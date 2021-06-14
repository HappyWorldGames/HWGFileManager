package com.happyworldgames.hwgfilemanager.data

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.webkit.MimeTypeMap
import java.io.File


class FileUtils {

    companion object {
        val imageExtensions = arrayOf(".png", ".jpg", ".gif", ".bmp")
        val videoExtensions = arrayOf(".mp4", ".3gp", ".mkv", ".ts", ".webm")
        val audioExtensions = arrayOf(".3gp", ".mp4", ".m4a", ".mp3", ".ogg", ".wav", ".mkv")

        fun sort(array: Array<File>) : ArrayList<File>{
            array.sort()

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

        fun checkIfFileHasExtension(name: String, extensions: Array<String>): Boolean {
            val check = name.lowercase()
            extensions.forEach { extension -> if(check.endsWith(extension)) return true }
            return false
        }

        fun createTrumbnailUtils(file: File): Bitmap {
            return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ThumbnailUtils.createVideoThumbnail(file, Size(512, 384), null)
            else ThumbnailUtils.createVideoThumbnail(file.absolutePath, MediaStore.Images.Thumbnails.MINI_KIND)
        }

        fun getMimeType(url: String?): String? {
            var type: String? = null
            val extension = MimeTypeMap.getFileExtensionFromUrl(url)
            if (extension != null) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }
            return type
        }
    }

}