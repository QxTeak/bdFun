package com.aritxonly.bdfun.core.domain

/** 动态页顶部的分类 Tab：全部 / 视频 / 番剧。key 与 FeedState.type 一致。 */
enum class DynamicFeedTab(val key: String, val title: String) {
    All("all", "全部"),
    Video("video", "视频"),
    Pgc("pgc", "番剧"),
}

object DynamicTabPolicy {
    val defaults: Set<String> = DynamicFeedTab.entries.map { it.key }.toSet()
    val defaultOrder: List<String> = DynamicFeedTab.entries.map { it.key }

    /** 「全部」和首页「推荐」一样始终保留，其它可以关。 */
    fun normalize(keys: Set<String>): Set<String> =
        DynamicFeedTab.entries.filter { it == DynamicFeedTab.All || it.key in keys }.map { it.key }.toSet()

    fun normalizeOrder(order: List<String>): List<String> {
        val known = defaultOrder
        val kept = order.filter { it in known }.distinct()
        return kept + known.filterNot { it in kept }
    }

    fun ordered(keys: Set<String>, order: List<String>): List<DynamicFeedTab> {
        val enabled = normalize(keys)
        return normalizeOrder(order).mapNotNull { key -> DynamicFeedTab.entries.firstOrNull { it.key == key } }
            .filter { it.key in enabled }
    }

    /** 当前选中的 Tab 被关掉时，落到第一个仍可见的 Tab。 */
    fun fallbackKey(keys: Set<String>, order: List<String>, current: String): String =
        ordered(keys, order).firstOrNull { it.key == current }?.key
            ?: ordered(keys, order).firstOrNull()?.key
            ?: DynamicFeedTab.All.key
}
