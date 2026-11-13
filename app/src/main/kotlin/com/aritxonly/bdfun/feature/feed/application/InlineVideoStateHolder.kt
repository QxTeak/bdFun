package com.aritxonly.bdfun.feature.feed.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.InlineVideoPreferences
import com.aritxonly.bdfun.core.domain.InlineVideoRepository
import com.aritxonly.bdfun.core.model.video.InlineVideoMedia
import com.aritxonly.bdfun.core.model.video.PlayInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class InlineVideoUiState(val info: PlayInfo? = null, val failed: Boolean = false)

internal class InlineVideoStateHolder(
    private val repository: InlineVideoRepository,
    private val bvid: String,
    private val preferences: () -> InlineVideoPreferences,
) {
    private val mutable = MutableStateFlow(InlineVideoUiState(repository.cachedPlayInfo(bvid)))
    val state = mutable.asStateFlow()
    private var closed = false
    private var loading = false
    fun close() { closed = true }

    suspend fun load() {
        val current = mutable.value
        if (closed || loading || current.info != null || current.failed || bvid.isBlank()) return
        loading = true
        try {
            val result = repository.load(bvid, preferences)
            if (closed) return
            mutable.value = when (result) {
                is Outcome.Success -> InlineVideoUiState(result.value)
                is Outcome.Failure -> InlineVideoUiState(failed = true)
            }
        } finally { loading = false }
    }
}

/** 与原预览保持相同的清晰度顺序，空字符串地址仍由原生播放器处理。 */
internal fun selectInlineVideoMedia(info: PlayInfo, preferredQuality: Int): InlineVideoMedia? {
    val quality = info.qualities.firstOrNull { it.id == preferredQuality }
        ?: info.qualities.firstOrNull { it.id == 64 }
        ?: info.qualities.lastOrNull()
    val url = quality?.videoUrl ?: info.fallbackUrl ?: return null
    return InlineVideoMedia(url, info.audioUrl)
}
