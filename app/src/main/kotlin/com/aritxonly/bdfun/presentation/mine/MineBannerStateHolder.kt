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

    suspend fun load(context: Context, profile: UserProfile?) {
        val candidates = (listOfNotNull(profile?.banner) + profile?.bannerFallbacks.orEmpty() + DEFAULT_SPACE_BANNER)
            .filter(String::isNotBlank).distinct()
        // 优先读磁盘/内存中的任一候选，弱网下不用等第一个 URL 超时。
        if (bitmap == null) for (url in candidates) {
            val cached = loadSpaceBannerBitmap(context, url, cacheOnly = true)
            if (cached != null) { bitmap = cached; break }
        }
        for (url in candidates) {
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
                return
            }
        }
    }
}
