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
    private var localEpisodes: List<VideoEpisode> = emptyList()
    private var localCollectionTitle: String = ""

/** 本地合集（稍后再看整页）：只覆盖当前详情里的 episodes/标题，不改接口字段。 */
    fun setLocalCollection(items: List<VideoItem>, title: String) {
        localEpisodes = items.mapIndexed { index, it ->
            VideoEpisode(
                bvid = it.bvid, aid = it.aid,
                // 稍后再看接口偶尔缺 cid；用唯一占位值保证合集条目能按 cid 对上，
                // 真正换集仍按 bvid 走。
                cid = it.cid.takeIf { cid -> cid > 0L } ?: -(index + 1L),
                title = it.title, cover = it.cover,
                duration = it.duration, play = it.play, danmaku = it.danmaku, pubdate = it.pubdate,
            )
        }
        localCollectionTitle = title
        publish { it }
    }

    private fun publish(transform: (VideoDetailReadState) -> VideoDetailReadState) {
        mutableState.update { transform(it).withLocalCollection() }
    }

    private fun VideoDetailReadState.withLocalCollection(): VideoDetailReadState {
        val current = detail ?: return this
        if (localEpisodes.size < 2) return this
        return copy(detail = current.copy(info = current.info.copy(
            episodes = localEpisodes,
            seasonTitle = localCollectionTitle,
        )))
    }

    fun restoreDetail(detail: VideoDetail) {
        publish { it.copy(detail = detail) }
    }

    fun restoreRelated(related: List<VideoItem>) {
        publish { it.copy(related = related.toList()) }
    }

    fun restore(detail: VideoDetail, playInfo: PlayInfo?, danmaku: List<DanmakuItem>, related: List<VideoItem>) {
        revision++
        streamRevision++
        danmakuRevision++
        publish { VideoDetailReadState(detail, related.toList(), playInfo, danmaku.toList(), loading = false) }
    }

    fun appendDanmaku(item: DanmakuItem) {
        publish { it.copy(danmaku = it.danmaku + item) }
    }

    suspend fun fetchDetail(bvid: String): Outcome<VideoDetail> = repository.videoDetail(bvid)

    suspend fun fetchRelated(bvid: String): List<VideoItem> = when (val result = repository.related(bvid)) {
        is Outcome.Success -> result.value
        is Outcome.Failure -> emptyList()
    }

    suspend fun resumePosition(detail: VideoDetail): Long = playback.resumePosition(detail).valueOr(0L)

    /**
     * 选集缓存用：解析某一集的详情和指定清晰度的播放信息。
     *
     * 多 P 视频直接复用当前详情（同一个 bvid 的不同 cid）；PGC 用 ep_id 重新取详情；
     * UGC 合集的其它单集按 bvid 取详情。失败（无版权/无音轨等）返回 null 由上层跳过。
     */
    suspend fun resolveCacheSource(
        bvid: String, cid: Long, durationSec: Int, epId: Long, qualityId: Int,
        codecPreference: String, cdnService: String,
    ): Pair<VideoDetail, PlayInfo>? {
        val current = mutableState.value.detail
        val detail = when {
            epId > 0L -> (repository.pgcEpisode(epId) as? Outcome.Success)?.value
            current != null && bvid == current.bvid -> current
            bvid.isNotBlank() -> (repository.videoDetail(bvid) as? Outcome.Success)?.value
            else -> current
        } ?: return null
        val request = VideoStreamRequest(
            detail = detail,
            quality = qualityId,
            cid = if (cid > 0L) cid else detail.cid,
            durationSec = durationSec.takeIf { it > 0 } ?: detail.duration,
            codecPreference = codecPreference,
            cdnService = cdnService,
        )
        val info = (playback.playInfo(request) as? Outcome.Success)?.value ?: return null
        return detail to info
    }

    suspend fun load(
        bvid: String,
        quality: Int,
        codec: String,
        cdn: String,
        resume: () -> Boolean,
        onPartSelected: (Long) -> Unit,
        onReady: (Long) -> Unit,
        requestedCid: Long = 0L,
        epId: Long = 0L,
        seasonId: Long = 0L,
    ) = coroutineScope {
        val request = ++revision
        val stream = ++streamRevision
        val commentsVersion = ++danmakuRevision
        publish { it.copy(loading = true, error = null, playError = null) }
        try {
            // PGC 只能按 ep_id/season_id 取详情：既要 64 位新 aid，也要拿整季剧集供合集列表使用。
            val result = if (epId > 0L || seasonId > 0L) repository.pgcEpisode(epId, seasonId)
            else repository.videoDetail(bvid)
            if (request != revision) return@coroutineScope false
            if (result is Outcome.Failure) {
                publish { it.copy(error = result.error) }
                return@coroutineScope false
            }
            val detail = (result as Outcome.Success).value
            restoreDetail(detail)
            val part = detail.forPlaybackPart(requestedCid)
            onPartSelected(part.cid)
            val shouldResume = resume()
            val play = async { playback.playInfo(VideoStreamRequest(part, quality, codecPreference = codec, cdnService = cdn)) }
            val danmaku = async { playback.danmaku(part.cid, part.duration).valueOr(emptyList()) }
            val related = async { if (detail.bvid.isBlank()) emptyList() else fetchRelated(detail.bvid) }
            val progress = async { if (shouldResume) resumePosition(part) else 0L }
            val source = play.await()
            val position = progress.await()
            if (request != revision) return@coroutineScope false
            publish { current ->
                if (stream == streamRevision) current.copy(
                    playInfo = source.valueOr(null), loading = false,
                    playError = (source as? Outcome.Failure)?.error,
                ) else current.copy(loading = false)
            }
            if (commentsVersion == danmakuRevision) onReady(position)
            val comments = danmaku.await()
            val recommendations = related.await()
            if (request != revision) return@coroutineScope false
            publish { it.copy(
                danmaku = if (commentsVersion == danmakuRevision) comments else it.danmaku,
                related = recommendations.ifEmpty { it.related },
            ) }
            true
        } finally {
            if (request == revision) publish { it.copy(loading = false) }
        }
    }

    /** 清晰度只换播放源；分 P 保持原先先换源、再加载弹幕的顺序。 */
    suspend fun switchStream(request: VideoStreamRequest, reloadDanmaku: Boolean): Boolean {
        val version = ++streamRevision
        val commentsVersion = if (reloadDanmaku) ++danmakuRevision else danmakuRevision
        val source = playback.playInfo(request)
        if (version != streamRevision) return false
        publish { it.copy(playInfo = source.valueOr(null), playError = (source as? Outcome.Failure)?.error) }
        if (reloadDanmaku) {
            val comments = playback.danmaku(request.cid, request.durationSec).valueOr(emptyList())
            if (commentsVersion == danmakuRevision) publish { it.copy(danmaku = comments) }
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

    /**
     * 番剧/影视换集：按 ep_id 重新取详情、播放源与弹幕；评论区由详情里的 epId 驱动重建。
     *
     * 与 UGC 合集的区别只在读取入口（ep_id 而非 bvid），换集后的时序保持一致。
     */
    suspend fun switchPgcEpisode(
        epId: Long,
        request: VideoStreamRequest,
        onDetail: (VideoDetail) -> Unit,
    ): Boolean {
        val selection = ++revision
        streamRevision++
        danmakuRevision++
        val result = repository.pgcEpisode(epId)
        if (selection != revision || result !is Outcome.Success) return false
        val detail = result.value
        restoreDetail(detail)
        onDetail(detail)
        if (!switchStream(request.copy(detail = detail), reloadDanmaku = true)) return false
        val stream = streamRevision
        val related = if (detail.bvid.isBlank()) emptyList() else fetchRelated(detail.bvid)
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
