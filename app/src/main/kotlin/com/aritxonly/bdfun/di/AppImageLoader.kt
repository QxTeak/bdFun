package com.aritxonly.bdfun.di

import android.content.Context
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.aritxonly.bdfun.infra.bili.BiliBackend
import okhttp3.OkHttpClient
import java.util.concurrent.atomic.AtomicBoolean

private val installed = AtomicBoolean(false)

/**
 * 全应用共用的 Coil 图片加载器。
 *
 * B 站图片 CDN 对部分稿件封面/番剧图做防盗链：不带 Referer 会返回 403，
 * 表现就是某些封面一直转圈、永远加载不出来。这里统一补上 Referer/UA，
 * 并显式给磁盘缓存，避免只在内存里成功、重启后又变回空白。
 */
internal fun installAppImageLoader(context: Context) {
    if (!installed.compareAndSet(false, true)) return
    val appContext = context.applicationContext
    val okHttp = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Referer", "https://www.bilibili.com/")
                .header("User-Agent", BiliBackend.UA)
                .build()
            chain.proceed(request)
        }
        .build()
    val loader = ImageLoader.Builder(appContext)
        .okHttpClient(okHttp)
        // 动态图的解码器默认不注册，GIF 会一直按静态图/失败处理；minSdk 33 可直接用 ImageDecoder。
        .components {
            add(ImageDecoderDecoder.Factory())
            add(GifDecoder.Factory())
        }
        .crossfade(true)
        .memoryCache {
            MemoryCache.Builder(appContext).maxSizePercent(0.15).build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(appContext.cacheDir.resolve("bili_image_cache"))
                .maxSizeBytes(256L * 1024 * 1024)
                .build()
        }
        .build()
    coil.Coil.setImageLoader(loader)
}
