package com.aritxonly.bdfun.core.domain

/**
 * 动态举报理由，编号与 PiliPlus 的 `ReportOptions.dynamicReport` 一致。
 *
 * `Other`（0）要求同时提交文字说明，界面上暂不提供，免得提交一个服务端必拒的空说明。
 */
enum class DynamicReportReason(val code: Int, val title: String) {
    Ads(4, "垃圾广告"), Provocation(8, "引战"), Pornography(1, "色情"),
    Attack(5, "人身攻击"), Illegal(3, "违法信息"), PoliticalRumor(9, "涉政谣言"),
    SocialRumor(10, "涉社会事件谣言"), FalseInfo(12, "虚假不实信息"), IllegalLink(13, "违法信息外链"),
}
