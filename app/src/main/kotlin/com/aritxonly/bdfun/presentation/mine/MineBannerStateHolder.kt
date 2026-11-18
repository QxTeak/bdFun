package com.aritxonly.bdfun.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.DEFAULT_SPACE_BANNER
import com.aritxonly.bdfun.loadSpaceBannerBitmap
import com.aritxonly.bdfun.core.model.user.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** 复用空间页的解码和Referer策略，图片加载仅属于展示状态。 */
internal class MineBannerStateHolder {
    var bitmap by mutableStateOf<Bitmap?>(null)
        private set

    suspend fun load(context: Context, profile: UserProfile?, mid: Long?) {
        val candidates = (listOfNotNull(profile?.banner) + profile?.bannerFallbacks.orEmpty() + DEFAULT_SPACE_BANNER)
            .filter(String::isNotBlank).distinct()
        val cache = ProfileImageCache(context)
        // 先用上次落盘的头图占位；自定义头图不会变来变去，这比默认图更接近最终结果。
        if (bitmap == null && mid != null && mid > 0L) cache.loadBanner(mid)?.let { bitmap = it }
        val cacheFresh = mid != null && mid > 0L && cache.isBannerFresh(mid)
        for (url in candidates) {
            val isDefault = isDefaultSpaceBanner(url)
            // 已经有新鲜缓存（多半是用户自己的头图）时不再去拉默认图，避免先闪默认再换自定义。
            if (isDefault && bitmap != null && cacheFresh) continue
            val loaded = try {
                loadSpaceBannerBitmap(context, url)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                null
            }
            currentCoroutineContext().ensureActive()
            if (loaded != null) {
                bitmap = loaded
                if (mid != null && mid > 0L && !isDefault) cache.saveBanner(mid, loaded)
                return
            }
        }
    }
}
