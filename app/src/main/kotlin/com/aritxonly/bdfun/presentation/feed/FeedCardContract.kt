package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Rect
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.teak.common.navigation.TeakMorphMenuState

internal data class FeedCardActions(
    val onOpen: () -> Unit,
    val onOpenVideo: (VideoItem) -> Unit,
    val onLike: () -> Unit,
    val onComment: () -> Unit,
    val onRepost: () -> Unit,
    val onOpenForwarded: (FeedItem) -> Unit = {},
    val onToggleMute: () -> Unit = {},
    /** 卡片右上省略号的形变菜单锚点；为 null 时不显示省略号（详情/空间等复用场景）。 */
    val moreMorphState: TeakMorphMenuState? = null,
    /** 省略号触发：先记录当前条目，再交给形变锚点自己展开。 */
    val onMore: (() -> Unit)? = null,
    /** 长按卡片：入参是按压点（窗口坐标），菜单从手指位置展开。 */
    val onLongPressRect: ((Rect) -> Unit)? = null,
)

internal data class FeedCardPresentation(
    val liked: Boolean,
    val autoPlay: Boolean = false,
    val muted: Boolean = true,
    val expanded: Boolean = false,
    val animatedOpen: Boolean = true,
)
