package com.aritxonly.bdfun.ui

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 图片保存到系统相册（Pictures/bdFun）。
 *
 * 不走 MediaStore 之外的私有目录：用户要的是能在相册里看到的图。
 * 下载用 `HttpURLConnection` 直接拉，因为图片 CDN 需要 Referer，
 * 而且这里不需要带 @Throws 之类额外依赖。
 */
internal object ImageSaver {
    private const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"

    /** 返回 (成功数, 总数)。逐张独立失败，不因为一张挂掉就整批放弃。 */
    suspend fun save(context: Context, urls: List<String>): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val targets = urls.filter { it.isNotBlank() }.distinct()
        var saved = 0
        targets.forEach { url ->
            if (runCatching { saveOne(context, url) }.isSuccess) saved++
        }
        saved to targets.size
    }

    private fun saveOne(context: Context, url: String) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Referer", "https://www.bilibili.com/")
        }
        try {
            connection.inputStream.use { input ->
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName(url))
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType(url))
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/bdFun",
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("无法创建相册条目")
                resolver.openOutputStream(uri)?.use { output -> input.copyTo(output) }
                    ?: error("无法写入相册")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun fileName(url: String): String {
        val suffix = url.substringAfterLast('.', "").substringBefore('?').lowercase()
        val ext = suffix.takeIf { it in setOf("jpg", "jpeg", "png", "webp", "gif") } ?: "jpg"
        return "bdFun_${System.currentTimeMillis()}_${kotlin.math.abs(url.hashCode())}.$ext"
    }

    private fun mimeType(url: String): String = when {
        url.contains(".png", ignoreCase = true) -> "image/png"
        url.contains(".webp", ignoreCase = true) -> "image/webp"
        url.contains(".gif", ignoreCase = true) -> "image/gif"
        else -> "image/jpeg"
    }
}
