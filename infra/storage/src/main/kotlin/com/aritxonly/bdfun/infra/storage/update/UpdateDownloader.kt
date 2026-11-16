package com.aritxonly.bdfun.infra.storage.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 系统下载管理器持有任务，离开页面或进程退出不会中断下载。 */
class UpdateDownloader(private val context: Context) {
    private val mutable = MutableStateFlow("")
    val state = mutable.asStateFlow()
    fun start(version: String, url: String?) {
        if (url == null || !url.startsWith("https://github.com/QxTeak/bdFun/releases/download/")) return
        try {
            val manager = context.getSystemService(DownloadManager::class.java)
            val preferences = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
            val key = "download_$version"
            val previous = preferences.getLong(key, -1)
            if (previous != -1L) manager.query(DownloadManager.Query().setFilterById(previous)).use { cursor ->
                if (cursor.moveToFirst() && cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) != DownloadManager.STATUS_FAILED) {
                    mutable.value = "已加入系统下载，请在通知栏或下载管理中查看并安装"
                    return
                }
            }
            val id = manager.enqueue(DownloadManager.Request(Uri.parse(url))
                .setTitle("bdFun $version")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "bdFun-$version.apk"))
            preferences.edit().putLong(key, id).apply()
            mutable.value = "正在后台下载，完成后点击系统下载通知安装"
        } catch (e: Exception) { mutable.value = "无法启动下载，请使用浏览器下载" }
    }
}
