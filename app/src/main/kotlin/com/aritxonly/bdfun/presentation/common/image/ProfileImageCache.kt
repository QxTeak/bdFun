package com.aritxonly.bdfun.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 头像 / 头图这类低频变化的图片单独落盘缓存。
 *
 * 接口请求照常进行，缓存只负责“打开页面时马上有东西可显示”：
 * 上次成功加载的图先上屏，网络返回新图后再覆盖缓存，
 * 这样冷启动时不会先闪默认头像 / 默认头图，再跳成用户自己的图。
 */
internal class ProfileImageCache(context: Context) {

    private val root = File(context.applicationContext.filesDir, "profile_images")

    fun avatarFile(mid: Long): File = File(root, "avatar_$mid.png")

    fun bannerFile(mid: Long): File = File(root, "banner_$mid.jpg")

    suspend fun loadAvatar(mid: Long): Bitmap? = read(avatarFile(mid))

    suspend fun saveAvatar(mid: Long, bitmap: Bitmap) =
        write(avatarFile(mid), bitmap, Bitmap.CompressFormat.PNG, 100)

    suspend fun loadBanner(mid: Long): Bitmap? = read(bannerFile(mid))

    /** 缓存是否还足够新；太旧的缓存不再阻止默认图覆盖，避免用户换回默认头图后永远显示旧图。 */
    fun isBannerFresh(mid: Long, maxAgeMillis: Long = 3L * 24 * 60 * 60 * 1000): Boolean {
        val file = bannerFile(mid)
        return file.exists() && System.currentTimeMillis() - file.lastModified() < maxAgeMillis
    }

    suspend fun saveBanner(mid: Long, bitmap: Bitmap) =
        write(bannerFile(mid), bitmap, Bitmap.CompressFormat.JPEG, 90)

    private suspend fun read(file: File): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)?.takeIf { !it.isRecycled }
    }

    private suspend fun write(
        file: File, bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int,
    ) = withContext(Dispatchers.IO) {
        if (bitmap.isRecycled) return@withContext
        runCatching {
            root.mkdirs()
            // 先写临时文件再替换，避免进程被杀时留下半张图。
            val tmp = File(file.parentFile, file.name + ".tmp")
            FileOutputStream(tmp).use { bitmap.compress(format, quality, it) }
            if (!tmp.renameTo(file)) {
                tmp.copyTo(file, overwrite = true)
                tmp.delete()
            }
        }
    }
}

/** B 站默认空间头图；不能拿它覆盖已经缓存的自定义头图。 */
internal fun isDefaultSpaceBanner(url: String): Boolean =
    url.isBlank() || url.contains("cb1c3ef50e22b6096fde67febe863494caefebad")

/**
 * 头像的网络加载：成功后由调用方写入 [ProfileImageCache]，
 * 缓存只做快速占位，接口请求本身必须保留。
 */
internal suspend fun loadProfileAvatarBitmap(
    context: Context, url: String,
): Bitmap? = withContext(Dispatchers.IO) {
    if (url.isBlank()) return@withContext null
    val request = coil.request.ImageRequest.Builder(context)
        .data(url)
        .size(240, 240)
        .scale(coil.size.Scale.FIT)
        .allowHardware(false)
        .build()
    (coil.Coil.imageLoader(context).execute(request) as? coil.request.SuccessResult)
        ?.drawable
        ?.toBitmap()
        ?.takeIf { !it.isRecycled }
}
