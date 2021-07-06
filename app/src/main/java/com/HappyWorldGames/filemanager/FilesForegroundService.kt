package com.happyworldgames.filemanager

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class FilesForegroundService : Service() {

    companion object {
        const val channelID = "Make with Files"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //val input = intent?.getStringExtra(Constants.inputExtra)
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification = NotificationCompat.Builder(this, channelID)
            .setContentTitle("notificationTitle")
            //.setContentText(input)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

}