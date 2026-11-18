package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class VideoMetadataState(
    val followers: Long = 0, val videos: Long = 0,
    val onlineCount: Long = 0, val onlineText: String = "",
    val subtitles: List<SubtitleTrack> = emptyList(), val selectedSubtitle: SubtitleTrack? = null,
    val subtitleCues: List<SubtitleCue> = emptyList(),
)

class VideoMetadataStateHolder(private val repository: VideoMetadataRepository) {
    private val mutableState = MutableStateFlow(VideoMetadataState())
    val state = mutableState.asStateFlow()
    private var authorRevision = 0L
    private var pageRevision = 0L
    private var subtitleRevision = 0L
    private var onlineRevision = 0L

    suspend fun loadAuthor(mid: Long, loggedIn: Boolean, fallbackFollowed: Boolean, onRelation: (Boolean) -> Unit) {
        val version = ++authorRevision
        val relation = if (loggedIn) repository.relation(mid).valueOr(null) else null
        if (version != authorRevision) return
        onRelation(if (relation != null) relation == 2 || relation == 6 else fallbackFollowed)
        val followers = repository.followers(mid).valueOr(0L)
        val videos = repository.videoCount(mid).valueOr(0L)
        if (version == authorRevision) mutableState.update { it.copy(followers = followers, videos = videos) }
    }

    suspend fun loadOnline(aid: Long, cid: Long) {
        val version = ++onlineRevision
        val online = repository.online(aid, cid).valueOr(0L to "")
        if (version == onlineRevision) mutableState.update { it.copy(onlineCount = online.first, onlineText = online.second) }
    }

    suspend fun loadSubtitles(aid: Long, cid: Long) {
        val version = ++pageRevision
        // 旧字幕响应不能在切 P 后恢复旧字幕；列表仍等接口返回后再按旧行为重置。
        subtitleRevision++
        val tracks = repository.subtitles(aid, cid).valueOr(emptyList())
        if (version == pageRevision) {
            subtitleRevision++
            mutableState.update { it.copy(subtitles = tracks, selectedSubtitle = null, subtitleCues = emptyList()) }
        }
    }

    suspend fun selectSubtitle(track: SubtitleTrack?) {
        val version = ++subtitleRevision
        mutableState.update { it.copy(selectedSubtitle = track) }
        if (track == null) {
            mutableState.update { it.copy(subtitleCues = emptyList()) }
            return
        }
        val cues = repository.subtitleCues(track.url).valueOr(emptyList())
        if (version == subtitleRevision) mutableState.update { it.copy(subtitleCues = cues) }
    }

    suspend fun reportDanmaku(report: DanmakuReport): Boolean =
        report.valid && repository.reportDanmaku(report) is Outcome.Success

    private val likedDanmaku = mutableSetOf<Pair<Long, Long>>()
    private val pendingLikes = mutableSetOf<Pair<Long, Long>>()

    suspend fun likeDanmaku(cid: Long, id: Long): Boolean {
        if (cid <= 0 || id <= 0) return false
        val key = cid to id
        if (key in likedDanmaku) return true
        if (!pendingLikes.add(key)) return false
        try {
            val success = repository.likeDanmaku(cid, id) is Outcome.Success
            if (success) likedDanmaku.add(key)
            return success
        } finally {
            pendingLikes.remove(key)
        }
    }

    suspend fun sendDanmaku(post: DanmakuPost): Boolean = repository.sendDanmaku(post) is Outcome.Success

    private fun <T> Outcome<T>.valueOr(fallback: T): T = when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> fallback
    }
}
