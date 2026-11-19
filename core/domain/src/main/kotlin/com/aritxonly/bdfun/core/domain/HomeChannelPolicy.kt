package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.home.HomeChannel

object HomeChannelPolicy {
    val defaults: Set<String> = HomeChannel.entries.map { it.key }.toSet()
    val defaultOrder: List<String> = HomeChannel.entries.map { it.key }

    fun normalize(keys: Set<String>): Set<String> =
        HomeChannel.entries.filter { it == HomeChannel.Recommend || it.key in keys }.map { it.key }.toSet()

    /** 顺序里只保留认识的频道，缺失的按默认顺序补在最后，避免老配置升级后丢 Tab。 */
    fun normalizeOrder(order: List<String>): List<String> {
        val known = defaultOrder
        val kept = order.filter { it in known }.distinct()
        return kept + known.filterNot { it in kept }
    }

    fun visible(keys: Set<String>): List<HomeChannel> =
        HomeChannel.entries.filter { it.key in normalize(keys) }

    fun ordered(keys: Set<String>, order: List<String>): List<HomeChannel> {
        val enabled = normalize(keys)
        return normalizeOrder(order).mapNotNull { key -> HomeChannel.entries.firstOrNull { it.key == key } }
            .filter { it.key in enabled }
    }

    fun selected(current: Int, keys: Set<String>, order: List<String> = defaultOrder): Int =
        current.takeIf { id -> ordered(keys, order).any { it.ordinal == id } } ?: HomeChannel.Recommend.ordinal
}
