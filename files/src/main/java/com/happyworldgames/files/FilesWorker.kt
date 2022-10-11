package com.happyworldgames.files

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import androidx.work.ListenableWorker.Result.Success
import androidx.work.ListenableWorker.Result.failure
import java.io.File

class FilesWorker(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    companion object {
        const val tagError = "error"

        const val cantRead = "can`t read"
        const val cantWrite = "can`t write"
        const val noInputData = "no input data"

        const val code = "code"

        const val dataText = "data"
        const val pathText = "path"
        const val resultText = "result"
        const val fromText = "from"
        const val toText = "to"

        const val progressText = "progress"
        const val goodText = "good"
        const val badText = "bad"

        fun returnError(errorText: String) = failure(Data.Builder().putString(tagError, errorText).build())

        fun startFilesWorker(context: Context, data: Data): OneTimeWorkRequest {
            val workRequest = OneTimeWorkRequest.Builder(FilesWorker::class.java).setInputData(data).build()
            WorkManager.getInstance(context).enqueue(workRequest)
            return workRequest
        }

        fun getListenerFilesWorker(appCompatActivity: AppCompatActivity, oneTimeWorkRequest: OneTimeWorkRequest, resultFun: (workInfo: WorkInfo) -> Unit) {
            return WorkManager.getInstance(appCompatActivity).getWorkInfoByIdLiveData(oneTimeWorkRequest.id).observe(appCompatActivity) {
                resultFun(it)
            }
        }
    }

    override fun doWork(): Result {
        val requestCode = inputData.getString(code)?: return returnError(noInputData)

        val filesWorkerRequest = try {
            FilesWorkerRequest.valueOf(requestCode)
        }catch (e: IllegalArgumentException) { return returnError(noInputData) }

        return when (filesWorkerRequest) {
            FilesWorkerRequest.WriteToFile -> writeToFileCommand()
            FilesWorkerRequest.WriteTextToFile -> writeTextToFileCommand()
            FilesWorkerRequest.ReadFromFile -> readFromFileCommand()
            FilesWorkerRequest.ReadTextFromFile -> readTextFromFileCommand()
            FilesWorkerRequest.CopyFile -> copyFileCommand()
            FilesWorkerRequest.MoveFile -> moveFileCommand()
        }
    }

    private fun writeToFileCommand(): Result {
        val path = inputData.getString(pathText)?: return returnError(noInputData)
        val data = inputData.getByteArray(dataText)?: return returnError(noInputData)

        return if (writeToFile(path, data)) Result.success()
        else failure()
    }
    private fun writeToFile(path: String, data: ByteArray): Boolean {
        val file = File(path)
        if (file.exists() && file.isDirectory) return false
        file.writeBytes(data)
        return true
    }

    private fun writeTextToFileCommand(): Result {
        val path = inputData.getString(pathText)?: return returnError(noInputData)
        val data = inputData.getString(dataText)?: return returnError(noInputData)

        return if (writeTextToFile(path, data)) Result.success()
        else failure()
    }
    private fun writeTextToFile(path: String, data: String): Boolean {
        val file = File(path)
        if (file.exists() && file.isDirectory) return false
        file.writeText(data)
        return true
    }

    private fun readFromFileCommand(): Result {
        val path = inputData.getString(pathText)?: return returnError(noInputData)

        val result = readFromFile(path)
        return Result.success(workDataOf(resultText to result))
    }
    private fun readFromFile(path: String): ByteArray {
        val file = File(path)
        if (!file.exists() || file.isDirectory) return byteArrayOf()
        return file.readBytes()
    }

    private fun readTextFromFileCommand(): Result {
        val path = inputData.getString(pathText)?: return returnError(noInputData)

        val result = readTextFromFile(path)
        return Result.success(workDataOf(resultText to result))
    }
    private fun readTextFromFile(path: String): String {
        val file = File(path)
        if (!file.exists() || file.isDirectory) return String()
        return file.readText()
    }

    private fun copyFileCommand(): Result {
        val fromFilePath = inputData.getString(fromText)?:return returnError(noInputData)
        val toFilePath = inputData.getString(toText)?:return returnError(noInputData)

        return copyFile(fromFilePath, toFilePath)
    }
    private fun copyFile(from: String, to: String): Result {
        val fromFile = File(from)
        val toFile = File(to)

        val toFolder = if (toFile.isFile) toFile.parentFile else toFile

        if (!toFolder.canWrite()) return returnError(cantWrite)
        if (!fromFile.canRead()) return returnError(cantRead)

        if (fromFile.isFile) {
            val newFile = File(toFolder, fromFile.name)
            fromFile.copyTo(newFile)

            val progressData = if (newFile.exists()) workDataOf(progressText to goodText)
            else workDataOf(progressText to badText)

            setProgressAsync(progressData)
        } else fromFile.listFiles()?.forEach {
            val localResult = copyFile(it.absolutePath, toFolder.absolutePath)
            if (localResult !is Success) return failure()
        }

        return if (toFolder.exists()) Result.success()
        else failure()
    }

    private fun moveFileCommand(): Result {
        val fromFilePath = inputData.getString(fromText)?:return returnError(noInputData)
        val toFilePath = inputData.getString(toText)?:return returnError(noInputData)

        return moveFile(fromFilePath, toFilePath)
    }
    private fun moveFile(from: String, to: String): Result {
        val fromFile = File(from)
        val toFile = File(to)

        val toFolder = if (toFile.isFile) toFile.parentFile else toFile

        if (!toFolder.canWrite()) return returnError(cantWrite)
        if (!fromFile.canRead()) return returnError(cantRead)

        if (fromFile.isFile) {
            val newFile = File(toFolder, fromFile.name)
            fromFile.copyTo(newFile)
            fromFile.delete()
        } else fromFile.listFiles()?.forEach {
            moveFile(it.absolutePath, toFolder.absolutePath)
        }

        return Result.success()
    }

}

enum class FilesWorkerRequest {
    WriteToFile, WriteTextToFile,
    ReadFromFile, ReadTextFromFile,
    CopyFile, MoveFile
}
