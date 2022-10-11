package com.happyworldgames.hwgfilemanager

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FilesWriteAndReadCheck {
    private val sdcardPath = File("/sdcard")

    @Test
    fun readDirFiles() {
        Assert.assertEquals(true, sdcardPath.listFiles()!!.size > 2)
    }

    @Test
    fun writeAndReadFile() {
        val testText = "Test Text 123"
        val file = File(sdcardPath, "Android/test.file")
        file.writeText(testText)
        Assert.assertEquals(testText, file.readText())
        if (file.exists()) file.delete()
    }

}