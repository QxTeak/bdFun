package com.aritxonly.bdfun.core.domain

/** 全屏里点「评论」时的呈现方式。 */
enum class FullscreenCommentPresentation {
    /** 全屏内右侧滑出评论面板，不动窗口朝向。 */
    InlinePanel,

    /** 退出全屏回详情页的评论 tab（竖屏本来是底部评论面板）。 */
    ExitToDetail,
}

/**
 * 横屏窗口里退出全屏，`requestedOrientation` 会交还给系统跟随传感器，
 * 手机此刻若不是横着拿，评论页会瞬间被甩成竖屏（issue #154）。
 * 所以横屏一律在全屏内就地打开面板；竖屏窗口仍走原来的底部评论面板。
 */
fun fullscreenCommentPresentation(windowLandscape: Boolean): FullscreenCommentPresentation =
    if (windowLandscape) FullscreenCommentPresentation.InlinePanel else FullscreenCommentPresentation.ExitToDetail