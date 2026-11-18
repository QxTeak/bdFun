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

    private val mutableVotes = MutableStateFlow<Map<Pair<Long, Long>, DanmakuVote>>(emptyMap())
    val danmakuVotes = mutableVotes.asStateFlow()
    private val pendingVotes = mutableSetOf<Pair<Long, Long>>()
    private val pendingReads = mutableSetOf<Pair<Long, Long>>()

    suspend fun loadDanmakuVotes(cid: Long, ids: List<Long>) {
        if (cid <= 0) return
        val keys = ids.distinct().filter { it > 0 }.map { cid to it }.filter {
            it !in mutableVotes.value && it !in pendingVotes && pendingReads.add(it)
        }
        if (keys.isEmpty()) return
        try {
            val result = repository.danmakuVotes(cid, keys.map { it.second })
            if (result is Outcome.Success) mutableVotes.update { state ->
                state + result.value.mapKeys { cid to it.key }.filterKeys { it !in state && it !in pendingVotes }
            }
        } finally {
            pendingReads.removeAll(keys.toSet())
        }
    }

    suspend fun likeDanmaku(cid: Long, id: Long): Boolean {
        if (cid <= 0 || id <= 0) return false
        val key = cid to id
        if (!pendingVotes.add(key)) return false
        val before = mutableVotes.value[key]
        val like = before?.liked != true
        var completed = false
        try {
            mutableVotes.update { it + (key to projectDanmakuVote(before, like)) }
            val result = repository.likeDanmaku(cid, id, like)
            if (result !is Outcome.Success) return false
            val confirmed = repository.danmakuVotes(cid, listOf(id))
            val server = (confirmed as? Outcome.Success)?.value?.get(id)?.takeIf { it.liked == result.value }
            mutableVotes.update { it + (key to (server ?: projectDanmakuVote(before, result.value))) }
            completed = true
            return true
        } finally {
            if (!completed) mutableVotes.update { if (before == null) it - key else it + (key to before) }
            pendingVotes.remove(key)
        }
    }

    private fun projectDanmakuVote(before: DanmakuVote?, liked: Boolean): DanmakuVote {
        val count = before?.likes?.let {
            when {
                before.liked == liked -> it
                liked && it < Long.MAX_VALUE -> it + 1
                !liked -> (it - 1).coerceAtLeast(0)
                else -> it
            }
        }
        return DanmakuVote(count, liked)
    }

    val danmakuModeration = DanmakuModerationStateHolder(repository)

    suspend fun sendDanmaku(post: DanmakuPost): Long? =
        (repository.sendDanmaku(post) as? Outcome.Success)?.value

    private fun <T> Outcome<T>.valueOr(fallback: T): T = when (this) {
        is Outcome.Success -> value
        is Outcome.Failure -> fallback
    }
}
