package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.home.HomeChannel

object HomeChannelPolicy {
    val defaults: Set<String> = HomeChannel.entries.map { it.key }.toSet()
    fun normalize(keys: Set<String>): Set<String> =
        HomeChannel.entries.filter { it == HomeChannel.Recommend || it.key in keys }.map { it.key }.toSet()
    fun visible(keys: Set<String>): List<HomeChannel> =
        HomeChannel.entries.filter { it.key in normalize(keys) }
    fun selected(current: Int, keys: Set<String>): Int =
        current.takeIf { id -> visible(keys).any { it.ordinal == id } } ?: HomeChannel.Recommend.ordinal
}
