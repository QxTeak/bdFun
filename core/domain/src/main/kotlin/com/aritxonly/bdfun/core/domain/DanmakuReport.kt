package com.aritxonly.bdfun.core.domain

/** 与 Hyper-PiliPlus 的 ReportOptions.danmakuReport 保持相同 reason 编号。 */
enum class DanmakuReportReason(val code: Int, val title: String) {
    Illegal(1, "违法违禁"), Pornography(2, "色情低俗"), Fraud(3, "赌博诈骗"),
    Attack(4, "人身攻击"), Privacy(5, "侵犯隐私"), Ads(6, "垃圾广告"),
    Provocation(7, "引战"), Spoiler(8, "剧透"), Spam(9, "恶意刷屏"),
    Irrelevant(10, "视频无关"), Youth(12, "青少年不良信息"), IllegalLink(13, "违法信息外链"),
    Other(11, "其它"),
}

data class DanmakuReport(val cid: Long, val id: Long, val reason: DanmakuReportReason, val content: String = "") {
    val valid: Boolean get() = cid > 0 && id > 0 && (reason != DanmakuReportReason.Other || content.isNotBlank())
}
