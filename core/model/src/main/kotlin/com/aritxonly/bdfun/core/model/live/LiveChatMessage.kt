package com.aritxonly.bdfun.core.model.live

/** 弹幕/互动区的一条消息。 */
data class LiveChatMessage(
    val id: Long,
    val kind: LiveChatKind,
    /** 发送者昵称；系统消息为空。 */
    val uname: String = "",
    val text: String = "",
    /** 粉丝牌：名称 / 等级 / 颜色（0 表示未取到，用主题粉）。 */
    val medalName: String? = null,
    val medalLevel: Int = 0,
    val medalColor: Int = 0,
    /** 昵称颜色（0xRRGGBB），null 表示用默认色。 */
    val nameColor: Long? = null,
    /** 超级留言的金额文字，例如「￥30」。 */
    val amountText: String? = null,
    /** 整条消息就是一张表情图时的图片地址。 */
    val emoteUrl: String? = null,
    /** 文内表情：token（如「[妙]」）→ 图片地址。 */
    val emotes: Map<String, String> = emptyMap(),
    /** 文内表情尺寸：token → 高（dp）。 */
    val emoteHeights: Map<String, Int> = emptyMap(),
)
