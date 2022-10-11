package com.happyworldgames.filemanager

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.happyworldgames.filemanager.databinding.RequestViewBinding
import com.happyworldgames.filemanager.view.MainView
import com.happyworldgames.files.FileRequest

class MainActivity : AppCompatActivity() {

    private val mainView by lazy { MainView(this) }
    private val permissionRequest = FileRequest.Permission(this) { isGranted ->
        if (isGranted) start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!FileRequest.Permission.hasPermission(this)) {
            val requestViewBinding = RequestViewBinding.inflate(layoutInflater)
            setContentView(requestViewBinding.root)
            requestViewBinding.requestButton.setOnClickListener {
                if (FileRequest.Permission.hasPermission(this)) start()
                else permissionRequest.requestPermission()
            }
        } else start()
    }

    private fun start() {
        setContentView(mainView.getView())
    }
}