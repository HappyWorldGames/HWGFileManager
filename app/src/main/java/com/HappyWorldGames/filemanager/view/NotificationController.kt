package com.happyworldgames.filemanager.view

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.happyworldgames.filemanager.MainActivity
import com.happyworldgames.filemanager.R
import com.happyworldgames.filemanager.data.ClipBoardData
import kotlin.random.Random

class NotificationController(private val context: Context) {
    private val notifyManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val pendingIntent: PendingIntent = Intent(context, MainActivity::class.java).let { notificationIntent ->
        PendingIntent.getActivity(context, 0, notificationIntent, 0)
    }

    fun createNotifyFile(clipBoardData: ClipBoardData): (progress: Int) -> Unit {
        val notifyId = Random(10293847).nextInt()
        val maxProgress = clipBoardData.files.size - 1

        val channelId = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel(channelName = clipBoardData.type.name) else ""
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
        notificationBuilder.setContentTitle("${clipBoardData.type.name.lowercase()} files")
            .setProgress(maxProgress, 0, false)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
        notifyManager.notify(notifyId, notificationBuilder.build())

        return { progress ->
            if(progress >= maxProgress) notificationBuilder.setProgress(0, 0, false).setContentText("Complete")
            else notificationBuilder.setProgress(maxProgress, progress, false)
            notifyManager.notify(notifyId, notificationBuilder.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String = "Work with Files", channelName: String): String{
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        notifyManager.createNotificationChannel(channel)
        return channelId
    }
}