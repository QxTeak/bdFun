package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class VideoDetailReadState(
    val detail: VideoDetail? = null,
    val related: List<VideoItem> = emptyList(),
    val playInfo: PlayInfo? = null,
    val danmaku: List<DanmakuItem> = emptyList(),
    val loading: Boolean = true,
    val error: AppError? = null,
    val playError: AppError? = null,
)

/** 缓存与离线数据由入口提供；首屏播放就绪后，推荐和弹幕继续并行补齐。 */
class VideoDetailStateHolder(
    private val repository: VideoRepository,
    initialRelated: List<VideoItem> = emptyList(),
    private val playback: VideoPlaybackRepository,
) {
    private val mutableState = MutableStateFlow(VideoDetailReadState(related = initialRelated.toList()))
    val state = mutableState.asStateFlow()
    private var revision = 0L
    private var streamRevision = 0L
    private var danmakuRevision = 0L

    fun restoreDetail(detail: VideoDetail) {
        mutableState.update { it.copy(detail = detail) }
    }

    fun restoreRelated(related: List<VideoItem>) {
        mutableState.update { it.copy(related = related.toList()) }
    }

    fun restore(detail: VideoDetail, playInfo: PlayInfo?, danmaku: List<DanmakuItem>, related: List<VideoItem>) {
        revision++
        streamRevision++
        danmakuRevision++
        mutableState.value = VideoDetailReadState(detail, related.toList(), playInfo, danmaku.toList(), loading = false)
    }

    fun appendDanmaku(item: DanmakuItem) {
        mutableState.update { it.copy(danmaku = it.danmaku + item) }
    }

    suspend fun fetchDetail(bvid: String): Outcome<VideoDetail> = repository.videoDetail(bvid)

    suspend fun fetchRelated(bvid: String): List<VideoItem> = when (val result = repository.related(bvid)) {
        is Outcome.Success -> result.value
        is Outcome.Failure -> emptyList()
    }

    suspend fun resumePosition(detail: VideoDetail): Long = playback.resumePosition(detail).valueOr(0L)

    suspend fun load(
        bvid: String,
        quality: Int,
        codec: String,
        cdn: String,
        resume: () -> Boolean,
        onPartSelected: (Long) -> Unit,
        onReady: (Long) -> Unit,
        requestedCid: Long = 0L,
    ) = coroutineScope {
        val request = ++revision
        val stream = ++streamRevision
        val commentsVersion = ++danmakuRevision
        mutableState.update { it.copy(loading = true, error = null, playError = null) }
        try {
            val result = repository.videoDetail(bvid)
            if (request != revision) return@coroutineScope false
            if (result is Outcome.Failure) {
                mutableState.update { it.copy(error = result.error) }
                return@coroutineScope false
            }
            val detail = (result as Outcome.Success).value
            restoreDetail(detail)
            val part = detail.forPlaybackPart(requestedCid)
            onPartSelected(part.cid)
            val shouldResume = resume()
            val play = async { playback.playInfo(VideoStreamRequest(part, quality, codecPreference = codec, cdnService = cdn)) }
            val danmaku = async { playback.danmaku(part.cid, part.duration).valueOr(emptyList()) }
            val related = async { fetchRelated(bvid) }
            val progress = async { if (shouldResume) resumePosition(part) else 0L }
            val source = play.await()
            val position = progress.await()
            if (request != revision) return@coroutineScope false
            mutableState.update { current ->
                if (stream == streamRevision) current.copy(
                    playInfo = source.valueOr(null), loading = false,
                    playError = (source as? Outcome.Failure)?.error,
                ) else current.copy(loading = false)
            }
            if (commentsVersion == danmakuRevision) onReady(position)
            val comments = danmaku.await()
            val recommendations = related.await()
            if (request != revision) return@coroutineScope false
            mutableState.update { it.copy(
                danmaku = if (commentsVersion == danmakuRevision) comments else it.danmaku,
                related = recommendations.ifEmpty { it.related },
            ) }
            true
        } finally {
            if (request == revision) mutableState.update { it.copy(loading = false) }
        }
    }

    /** 清晰度只换播放源；分 P 保持原先先换源、再加载弹幕的顺序。 */
    suspend fun switchStream(request: VideoStreamRequest, reloadDanmaku: Boolean): Boolean {
        val version = ++streamRevision
        val commentsVersion = if (reloadDanmaku) ++danmakuRevision else danmakuRevision
        val source = playback.playInfo(request)
        if (version != streamRevision) return false
        mutableState.update { it.copy(playInfo = source.valueOr(null), playError = (source as? Outcome.Failure)?.error) }
        if (reloadDanmaku) {
            val comments = playback.danmaku(request.cid, request.durationSec).valueOr(emptyList())
            if (commentsVersion == danmakuRevision) mutableState.update { it.copy(danmaku = comments) }
        }
        return version == streamRevision
    }

    suspend fun switchEpisode(bvid: String, request: VideoStreamRequest, onDetail: (VideoDetail) -> Unit): Boolean {
        val selection = ++revision
        streamRevision++
        danmakuRevision++
        val result = repository.videoDetail(bvid)
        if (selection != revision || result !is Outcome.Success) return false
        val detail = result.value
        restoreDetail(detail)
        onDetail(detail)
        if (!switchStream(request.copy(detail = detail), reloadDanmaku = true)) return false
        val stream = streamRevision
        val related = fetchRelated(bvid)
        if (selection != revision || stream != streamRevision) return false
        restoreRelated(related)
        return true
    }

    private fun <T> Outcome<T>.valueOr(fallback: T): T = when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> fallback
    }
}

/** 只使用当前稿件里的分 P；详情本身仍保留接口给出的默认 CID。 */
internal fun VideoDetail.forPlaybackPart(requestedCid: Long): VideoDetail {
    val page = requestedCid.takeIf { it > 0L }?.let { wanted -> pages.firstOrNull { it.cid == wanted } }
        ?: pages.firstOrNull { it.cid == cid }
        ?: return this
    return copy(info = info.copy(cid = page.cid, duration = page.duration.takeIf { it > 0 } ?: duration))
}
