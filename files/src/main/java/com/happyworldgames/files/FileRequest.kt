package com.happyworldgames.files

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.io.File

class FileRequest(var path: String) {

    fun createFile(): Boolean {
        return File(path).createNewFile()
    }
    fun createFolder(): Boolean {
        return File(path).mkdirs()
    }

    fun deleteFile(): Boolean {
        return File(path).delete()
    }

    fun list(): Array<String> {
        return File(path).list() ?: arrayOf()
    }
    fun filesList(): List<FileRequest> {
        return File(path).listFiles()?.map { FileRequest(it.absolutePath) }?: listOf()
    }

    fun writeToFile(context: Context, data: ByteArray) {
        FilesWorker.startFilesWorker(context, workDataOf(
            FilesWorker.code to FilesWorkerRequest.WriteToFile,
            FilesWorker.pathText to path, FilesWorker.dataText to data)
        )
    }
    fun writeTextToFile(context: Context, data: String) {
        FilesWorker.startFilesWorker(context, workDataOf(
            FilesWorker.code to FilesWorkerRequest.WriteTextToFile,
            FilesWorker.pathText to path, FilesWorker.dataText to data)
        )
    }

    fun readFromFile(appCompatActivity: AppCompatActivity, resultFun: (data: ByteArray) -> Unit) {
        val context = appCompatActivity as Context
        val oneTimeWorkRequest = FilesWorker.startFilesWorker(context, workDataOf(
            FilesWorker.code to FilesWorkerRequest.ReadFromFile,
            FilesWorker.pathText to path)
        )

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(oneTimeWorkRequest.id).observe(appCompatActivity) {
            if (it.state == WorkInfo.State.SUCCEEDED) {
                val result = it.outputData.getByteArray(FilesWorker.resultText) ?: byteArrayOf()
                resultFun(result)
            }
        }
    }

    fun readTextFromFile(appCompatActivity: AppCompatActivity, resultFun: (data: String) -> Unit) {
        val context = appCompatActivity as Context
        val oneTimeWorkRequest = FilesWorker.startFilesWorker(context, workDataOf(
            FilesWorker.code to FilesWorkerRequest.ReadTextFromFile,
            FilesWorker.pathText to path)
        )

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(oneTimeWorkRequest.id).observe(appCompatActivity) {
            if (it.state == WorkInfo.State.SUCCEEDED) {
                val result = it.outputData.getString(FilesWorker.resultText) ?: String()
                resultFun(result)
            }
        }
    }

    fun copyFile(appCompatActivity: AppCompatActivity, from: String, to: String, callBack: (progress: Int) -> Unit) {
        val context = appCompatActivity as Context

        val oneTimeWorkRequest = FilesWorker.startFilesWorker(context, workDataOf(
            FilesWorker.code to FilesWorkerRequest.CopyFile,
            FilesWorker.fromText to from, FilesWorker.toText to to)
        )

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(oneTimeWorkRequest.id).observe(appCompatActivity) {
            if (it.state != WorkInfo.State.RUNNING) return@observe
            val progressText = it.progress.getString(FilesWorker.progressText)
            if (progressText == FilesWorker.goodText) {
                callBack(1)
            }
        }
    }

    fun moveFile(context: Context, from: String, to: String) {
        FilesWorker.startFilesWorker(context, workDataOf(
            FilesWorker.code to FilesWorkerRequest.MoveFile,
            FilesWorker.fromText to from, FilesWorker.toText to to)
        )
    }


    class Permission(appCompatActivity: AppCompatActivity, result: (isGranted: Boolean) -> Unit) {

        companion object {
            fun hasPermission(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager())
        }

        val requestPermissionLaunch = appCompatActivity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            result(isGranted)
        }
        val requestResultLaunch = appCompatActivity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result(hasPermission(appCompatActivity))
        }
        val context = appCompatActivity as Context

        fun requestPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLaunch.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    try {
                        val uri: Uri = Uri.parse("package:${context.packageName}")
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                        requestResultLaunch.launch(intent)
                    } catch (ex: Exception) {
                        val intent = Intent()
                        intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                        requestResultLaunch.launch(intent)
                    }
                }
            }
        }
    }

}