package com.aritxonly.bdfun.core.model.video

data class Quality(
    val id: Int,
    val label: String,
    val width: Int,
    val height: Int,
    val frameRate: String,
    val codec: String,
    val videoUrl: String,
    /** CDN 替换前的原始地址，CDN 403 时用来重试。 */
    val rawVideoUrl: String = "",
    /** 码率（bps）；DASH 自适应源在 MPD 里必须写这个字段。 */
    val bandwidth: Long = 0,
    /** fMP4 的初始化段/索引段字节范围；两段都有才能拼本地 MPD，缺一个就退回渐进流。 */
    val initRange: String? = null,
    val indexRange: String? = null,
)
