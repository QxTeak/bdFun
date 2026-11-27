package com.aritxonly.bdfun.ui

/**
 * 「不感兴趣」撤销态的卡片参数。
 *
 * 卡片本身已经有播放进度、长按、更多、形变菜单等输入，参数超过 12 个会被尺寸审计拦下；
 * 这四个字段总是成套出现，收成一组既满足约束，也避免调用方只传一半。
 */
internal data class VideoCardUndo(
    /** 封面重度模糊：只在撤销窗口内为 true。 */
    val blurred: Boolean = false,
    /** 只保留卡内撤销按钮，标题/UP/省略号全部隐藏。 */
    val undoOnly: Boolean = false,
    val onUndo: (() -> Unit)? = null,
    /** 模糊封面上显示的反馈提示。 */
    val undoMessage: String? = null,
)

/** 批量管理时的勾选态：勾选圈开关与当前是否选中。 */
internal data class VideoCardSelection(
    val enabled: Boolean = false,
    val selected: Boolean = false,
)
