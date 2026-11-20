package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem

/** 失败通过 Outcome.Failure 返回；取消协程必须继续抛出，避免误显示为网络错误。 */
interface VideoRepository {
    suspend fun videoDetail(bvid: String): Outcome<VideoDetail>
    suspend fun related(bvid: String): Outcome<List<VideoItem>>

    /**
     * PGC 剧集按 ep_id（或只有 season_id 时按季）读取。
     *
     * 番剧/影视的评论挂在 ep_id 上，播放地址也要带 ep_id，不能只拿 bvid 当普通稿件。
     * 未实现该能力的数据源（离线/测试替身）返回失败，由调用方决定是否回退。
     */
    suspend fun pgcEpisode(epId: Long, seasonId: Long = 0L): Outcome<VideoDetail> =
        Outcome.Failure(AppError.Unknown("当前数据源不支持番剧"))
}
