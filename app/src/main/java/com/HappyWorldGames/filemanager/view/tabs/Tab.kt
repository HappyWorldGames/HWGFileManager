package com.happyworldgames.filemanager.view.tabs

interface TabData {
    var name: String
}

class MainTabData : TabData {
    override var name = "Main"
}
class FilesTabData(var path: String) : TabData {
    override var name = "Files"
}