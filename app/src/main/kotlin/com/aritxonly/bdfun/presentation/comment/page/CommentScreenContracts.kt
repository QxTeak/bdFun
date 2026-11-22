package com.aritxonly.bdfun.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class CommentScreenRequest(
    val aid: Long, val upMid: Long, val title: String, val bvid: String = "",
    /** 动态用 type=17 + id_str，视频用 type=1 + aid。 */
    val commentOid: String = aid.toString(), val commentType: Int = 1,
    val focusRootRpid: Long = 0L, val focusTargetRpid: Long = 0L,
)
internal data class CommentScreenLayout(
    val bare: Boolean = false, val topInset: Dp = 0.dp, val bottomPadding: Dp = 96.dp,
    val trackBackProgress: Boolean = false, val showLoading: Boolean = true,
    val threadPlayerBottom: Dp? = null, val threadPaneWidth: Dp? = null,
    val playerFullscreen: Boolean = false, val flatList: Boolean = false,
    /** 视频详情竖屏主评论区：收紧缩进播放器下方的标题留白（横屏分栏保持原样）。 */
    val compactHeaderSpacing: Boolean = false,
    /** 评论区作为独立可见页面时注册「回到顶部」；视频详情双栏/评论 tab 由调用方按可见性传。 */
    val scrollTop: Boolean = false,
    /** 动态详情这类沉浸页：列表滚到状态栏后面，而不是被状态栏高度截断。 */
    val immersiveStatusBar: Boolean = false,
)
internal typealias CommentScreenHeader = LazyListScope.() -> Unit

/** 评论区统一字号：固定为原来的「110%」档，不再提供调节。 */
internal const val COMMENT_FONT_SCALE = 1.1f
