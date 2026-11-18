package com.aritxonly.bdfun.core.model.video

data class DanmakuItem(
    val timeMs: Long,
    val mode: Int,
    val color: Int,
    val size: Float,
    val text: String,
    /** 服务端弹幕 id，用于热门弹幕去重 / 点赞。 */
    val id: Long = 0L,
    /** 服务端点赞数（分段弹幕接口 like 字段）。 */
    val likeCount: Long = 0L,
    /** 服务端合并的重复弹幕数量（count 字段）。 */
    val count: Int = 0,
    /** 自己刚发送的弹幕：渲染时加圆角底。 */
    val self: Boolean = false,
    val userHash: String = "",
)
