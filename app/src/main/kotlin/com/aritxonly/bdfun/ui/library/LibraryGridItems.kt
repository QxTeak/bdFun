package com.aritxonly.bdfun.ui

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.bdfun.feature.library.application.HistoryCategory

/**
 * 收藏/历史/稍后再看的列表项：首屏骨架、视频行、空态和分页尾巴。
 *
 * 放在 `ui/` 而不是 `presentation/library/`：这里要显式引用 `data.VideoItem`，
 * 而架构守卫禁止表现层直接引用基础设施类型。页面只负责传数据，不在这里做业务。
 */
internal fun LazyGridScope.libraryModeGridItems(
    content: LibraryModeContent, layout: LibraryModeLayout, actions: LibraryModeActions,
    historyCategory: HistoryCategory, rows: List<VideoItem>,
    queueRows: List<VideoItem>, itemModifier: Modifier,
    particle: com.aritxonly.teak.common.motion.TeakParticleBurstState? = null,
) {
    val emptyFirstLoad = content.loading && rows.isEmpty()
    if (emptyFirstLoad) {
        videoResultSkeleton(keyPrefix = "library-skeleton-${layout.mode}")
    }
    videoResultRows(
        videos = rows,
        keyPrefix = "library-${layout.mode}-",
        // 队列必须在点击时、跳转前就准备好：一镜到底会由系统动画自己起 VideoActivity，
        // onOpenVideo 只是动画失败时的兜底，正常路径根本不执行，写在那里等于没准备。
        onBeforeOpen = { video ->
            if (!content.batchMode) {
                // 稍后再看把整页当成一个本地合集；收藏/历史只保留普通连播队列。
                if (layout.mode == 2) {
                    actions.onPrepareCollection(queueRows, video.bvid, "稍后再看")
                } else {
                    actions.onPrepareQueue(queueRows, video.bvid, "")
                }
            }
        },
        onOpenVideo = { video ->
            if (content.batchMode) {
                actions.onToggleSelected(video.bvid)
            } else {
                actions.onOpenVideo(video)
            }
        },
        onVideoMenu = if (content.batchMode) null else actions.onVideoMenu,
        showSelection = content.batchMode,
        selectedIds = content.selected,
        // 批量勾选时不要一镜到底放大动画。
        enableOneTake = !content.batchMode,
        itemModifier = itemModifier,
        particleState = particle,
    )
    if (!emptyFirstLoad && rows.isEmpty() && !content.loading && content.error == null) {
        libraryEmptyItem(layout.mode, historyCategory)
    }
    libraryGridFooter(content, layout, actions)
}
