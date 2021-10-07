package com.happyworldgames.filemanager

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.happyworldgames.filemanager.data.ClipBoardData
import com.happyworldgames.filemanager.data.DataBase
import com.happyworldgames.filemanager.data.FileUtils
import com.happyworldgames.filemanager.view.NotificationController
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.CoroutineContext

class FilesForegroundService : Service(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()

    lateinit var clipBoardData: ClipBoardData
    private val notificationController by lazy{ NotificationController(this) }
    lateinit var onUpdateNotify: (progress: Int) -> Unit

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent == null) return START_STICKY

        clipBoardData = DataBase.clipBoardBase[intent.getIntExtra("clip_board_data_index", -1)]
        onUpdateNotify = notificationController.createNotifyFile(clipBoardData)

        startForeground(notificationController.notifyId, notificationController.notification)

        launch(Dispatchers.IO) {
            paste(intent)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private suspend fun paste(intent: Intent?) {
        if(intent == null) return

        val currentPage = intent.getIntExtra("current_page", -1)
        val requestOverWrite = FileUtils.staticRequestOverWrite
        FileUtils.staticRequestOverWrite = null

        val type = clipBoardData.type
        val files = clipBoardData.files

        var request = true
        var overWrite = false

        var breakFor = false
        var waitRequest = false
        files.values.forEachIndexed pasteFor@{ progress, it ->
            onUpdateNotify(progress)
            val fileTo = File(FileUtils.getDataItemFilesFromIndex(currentPage).path, it.name)
            //need stop requestOverWrite when his show, and start when hide
            if(fileTo.exists() && request){
                waitRequest = true
                if(requestOverWrite != null) requestOverWrite(fileTo) { result ->
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

        stopSelf()
    }
}