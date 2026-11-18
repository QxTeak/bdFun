package com.aritxonly.bdfun.core.model.home

/** 稳定键用于存储；不能用用户当前可见列表的下标标识频道。 */
enum class HomeChannel(val key: String, val title: String) {
    Recommend("recommend", "推荐"), Live("live", "直播"), Bangumi("bangumi", "番剧"),
    Popular("popular", "热门"), Cinema("cinema", "影视"),
}
