package com.happyworldgames.files

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result.Success
import androidx.work.testing.TestWorkerBuilder
import androidx.work.workDataOf
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class FilesWorkerTest {
    private lateinit var context: Context
    private lateinit var executor: Executor

    private val temporaryFolder = TemporaryFolder()
    private val testFolder = arrayOf("sdcard", "HWGTest")
    private val testFolderString = testFolder.joinToString("/", "/")
    private var testFolderFile: File? = null

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()

        testFolderFile = temporaryFolder.newFolder("sdcard", "HWGTest")
    }

    @After
    fun after() {
        testFolderFile?.deleteRecursively()
    }

    @Test
    fun testDoWorkerFailure() {
        val worker = TestWorkerBuilder<FilesWorker>(
            context = context,
            executor = executor,
            inputData = workDataOf(FilesWorker.code to "TestNull")
        ).build()

        val result = worker.doWork()
        assertThat(result, `is`(FilesWorker.returnError(FilesWorker.noInputData)))
    }

    @Test
    fun testWriteToFile0CharSuccess() {
        val worker = TestWorkerBuilder<FilesWorker>(
            context = context,
            executor = executor,
            inputData = workDataOf(
                FilesWorker.code to FilesWorkerRequest.WriteTextToFile.name,
                FilesWorker.pathText to "$testFolderString/testFile.txt",
                FilesWorker.dataText to String()
            )
        ).build()

        val result = worker.doWork()
        assertThat(result, `is`(Success()))

        testReadFromFile0CharSuccess()
    }
    private fun testReadFromFile0CharSuccess() {
        val worker = TestWorkerBuilder<FilesWorker>(
            context = context,
            executor = executor,
            inputData = workDataOf(
                FilesWorker.code to FilesWorkerRequest.ReadTextFromFile,
                FilesWorker.pathText to "$testFolderString/testFile.txt"
            )
        ).build()

        val result = worker.doWork().outputData.getString(FilesWorker.resultText)
        if (result == null) assert(false)
        else assertThat(result, `is`(""))
    }

    @Test
    fun testWriteToFile40CharSuccess() {
        var testText = String()
        for (i in 0..40)
            testText += Random.nextInt(255).toChar()

        val worker = TestWorkerBuilder<FilesWorker>(
            context = context,
            executor = executor,
            inputData = workDataOf(
                FilesWorker.code to FilesWorkerRequest.WriteTextToFile,
                FilesWorker.pathText to "$testFolderString/testFile.txt",
                FilesWorker.dataText to testText
            )
        ).build()

        val result = worker.doWork()
        assertThat(result, `is`(Success()))
    }

    @Test
    fun testWriteToFile1000CharSuccess() {
        var testText = String()
        for (i in 0..1000)
            testText += Random.nextInt(255).toChar()

        val worker = TestWorkerBuilder<FilesWorker>(
            context = context,
            executor = executor,
            inputData = workDataOf(
                FilesWorker.code to FilesWorkerRequest.WriteTextToFile,
                FilesWorker.pathText to "$testFolderString/testFile.txt",
                FilesWorker.dataText to testText
            )
        ).build()

        val result = worker.doWork()
        assertThat(result, `is`(Success()))
    }

    @Test
    fun testWriteToFile0BytesSuccess() {
        val worker = TestWorkerBuilder<FilesWorker>(
            context = context,
            executor = executor,
            inputData = workDataOf(
                FilesWorker.code to FilesWorkerRequest.WriteToFile,
                FilesWorker.pathText to "$testFolderString/testFile.txt",
                FilesWorker.dataText to byteArrayOf()
            )
        ).build()

        val result = worker.doWork()
        assertThat(result, `is`(Success()))
    }

    @Test
    fun testWriteToFile1000BytesSuccess() {
        val worker = TestWorkerBuilder<FilesWorker>(
            context = context,
            executor = executor,
            inputData = workDataOf(
                FilesWorker.code to FilesWorkerRequest.WriteToFile,
                FilesWorker.pathText to "$testFolderString/testFile.txt",
                FilesWorker.dataText to ByteArray(1000) { Random.nextInt(Byte.MAX_VALUE.toInt()).toByte() }
            )
        ).build()

        val result = worker.doWork()
        assertThat(result, `is`(Success()))
    }

    @Test
    fun testWriteToFile100MBSuccess() {
        val worker = TestWorkerBuilder<FilesWorker>(
            context = context,
            executor = executor,
            inputData = workDataOf(
                FilesWorker.code to FilesWorkerRequest.WriteToFile,
                FilesWorker.pathText to "$testFolderString/testFile.txt",
                FilesWorker.dataText to ByteArray(100_000) { Random.nextInt(Byte.MAX_VALUE.toInt()).toByte() }
            )
        ).build()

        val result = worker.doWork()
        assertThat(result, `is`(Success()))
    }

    @Test
    fun testWriteToFile1000MBSuccess() {
        val worker = TestWorkerBuilder<FilesWorker>(
            context = context,
            executor = executor,
            inputData = workDataOf(
                FilesWorker.code to FilesWorkerRequest.WriteToFile,
                FilesWorker.pathText to "$testFolderString/testFile.txt",
                FilesWorker.dataText to ByteArray(1000_000) { Random.nextInt(Byte.MAX_VALUE.toInt()).toByte() }
            )
        ).build()

        val result = worker.doWork()
        assertThat(result, `is`(Success()))
    }
}