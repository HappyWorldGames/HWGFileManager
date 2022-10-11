package com.happyworldgames.files

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileRequestTest {

    @Test
    fun createFile() {
        val file = File("/sdcard/Android/testFile.test")
        val fileRequest = FileRequest(file.absolutePath)
        fileRequest.createFile()
        Assert.assertEquals(true, file.exists())
    }

    @Test
    fun createFolder() {
        val folder = File("/sdcard/Android/testFolder")
        val folderRequest = FileRequest(folder.absolutePath)
        folderRequest.createFolder()
        Assert.assertEquals(true, folder.exists())
    }

}