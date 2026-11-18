package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.model.video.*

data class VideoCacheEntry(
    val detail: VideoDetail, val playInfo: PlayInfo?,
    val danmaku: List<DanmakuItem>, val related: List<VideoItem>,
    val playbackCid: Long = detail.cid,
) {
    internal fun matchesPart(requestedCid: Long): Boolean = playbackCid == detail.forPlaybackPart(requestedCid).cid
}
data class VideoReadCacheOwner(val mid: Long?, val revision: Long)

/** 进程持有有界缓存，页面只取得当前身份的句柄，不能重新选择身份写入。 */
class VideoReadCache(private val currentOwner: () -> VideoReadCacheOwner) {
    private var owner: VideoReadCacheOwner? = null
    private var closed = false
    private val recommendations = HashMap<String, List<VideoItem>>()
    private val videos = LinkedHashMap<String, VideoCacheEntry>()
    fun scope(): VideoReadCacheScope = synchronized(this) {
        check(!closed) { "视频读取缓存已关闭" }
        VideoReadCacheScope(this, currentOwner())
    }
    fun close() = synchronized(this) {
        closed = true
        recommendations.clear()
        videos.clear()
    }

    private fun <T> access(request: VideoReadCacheOwner, block: () -> T): T? = synchronized(this) {
        if (closed) return@synchronized null
        val current = currentOwner()
        if (owner != current) {
            recommendations.clear()
            videos.clear()
            owner = current
        }
        if (request == current) block() else null
    }
    internal fun related(owner: VideoReadCacheOwner, bvid: String) = access(owner) { recommendations[bvid] }
    internal fun video(owner: VideoReadCacheOwner, bvid: String) = access(owner) { videos[bvid] }
    internal fun putRelated(owner: VideoReadCacheOwner, bvid: String, value: List<VideoItem>) {
        access(owner) {
            if (value.isNotEmpty()) recommendations[bvid] = value
            while (recommendations.size > 12) recommendations.remove(recommendations.keys.first())
        }
    }
    internal fun putVideo(owner: VideoReadCacheOwner, bvid: String, entry: VideoCacheEntry) {
        access(owner) {
            videos[bvid] = entry
            while (videos.size > 6) videos.remove(videos.keys.first())
        }
    }
    internal fun updateVideo(owner: VideoReadCacheOwner, bvid: String, transform: (VideoDetail) -> VideoDetail) {
        access(owner) { videos[bvid]?.let { videos[bvid] = it.copy(detail = transform(it.detail)) } }
    }
    internal fun removeRelated(owner: VideoReadCacheOwner, bvid: String) {
        access(owner) { recommendations.remove(bvid) }
    }
}

/** 关闭只阻止本页面迟到访问；旋转仍保留同账号缓存供新页面读取。 */
class VideoReadCacheScope internal constructor(
    private val cache: VideoReadCache, private val owner: VideoReadCacheOwner,
) {
    private var closed = false
    fun related(bvid: String) = if (closed) null else cache.related(owner, bvid)
    fun video(bvid: String) = if (closed) null else cache.video(owner, bvid)
    fun putRelated(bvid: String, value: List<VideoItem>) { if (!closed) cache.putRelated(owner, bvid, value) }
    fun putVideo(bvid: String, entry: VideoCacheEntry) { if (!closed) cache.putVideo(owner, bvid, entry) }
    fun updateVideo(bvid: String, transform: (VideoDetail) -> VideoDetail) {
        if (!closed) cache.updateVideo(owner, bvid, transform)
    }
    fun close(clearRelatedFor: String? = null) {
        if (closed) return
        closed = true
        if (!clearRelatedFor.isNullOrBlank()) cache.removeRelated(owner, clearRelatedFor)
    }
}
