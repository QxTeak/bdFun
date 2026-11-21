package com.aritxonly.bdfun.core.model.video

enum class DanmakuCommandKind { Notice, Link, Follow, Vote, Grade }

data class DanmakuCommandOption(
    val value: Int,
    val label: String,
    /** 该选项当前票数；命令下发时通常为 0，投完票再请求统计补全。 */
    val count: Long = 0,
)

data class DanmakuCommand(
    val id: Long,
    val kind: DanmakuCommandKind,
    val text: String,
    val startMs: Long,
    val durationMs: Long,
    val interactionId: Long = 0,
    val options: List<DanmakuCommandOption> = emptyList(),
    val selected: Int? = null,
    val participants: Long? = null,
    val average: Double? = null,
    val presentation: DanmakuCommandPlacement = DanmakuCommandPlacement(),
) {
    val linkBvid get() = presentation.linkBvid
    val linkAid get() = presentation.linkAid
    val x get() = presentation.x
    val y get() = presentation.y
    val attentionType get() = presentation.attentionType
}

data class DanmakuCommandPlacement(
    val linkBvid: String = "",
    val linkAid: Long = 0,
    val x: Float? = null,
    val y: Float? = null,
    val attentionType: Int = 0,
)
