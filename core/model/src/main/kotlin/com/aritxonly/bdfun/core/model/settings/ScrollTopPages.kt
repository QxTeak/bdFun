package com.aritxonly.bdfun.core.model.settings

/** 「回到顶部」按钮可以单独开 / 关的页面。key 同时用于设置存储和页面注册。 */
val scrollTopPageEntries = listOf(
    "home" to "首页",
    "dynamic" to "动态页",
    "live" to "直播页",
    "pgc" to "番剧页",
    "comment" to "评论区",
    "space" to "个人空间",
    "library" to "收藏 / 历史 / 稍后再看",
    "message" to "消息页",
    "message_detail" to "消息详情页",
)
val allScrollTopPages: Set<String> = scrollTopPageEntries.map { it.first }.toSet()
