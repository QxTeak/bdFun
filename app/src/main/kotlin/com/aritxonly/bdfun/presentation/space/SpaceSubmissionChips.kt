package com.aritxonly.bdfun

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.navigation.TeakChip
import com.aritxonly.teak.common.navigation.TeakSlidingChipRow

/**
 * 投稿页的筛选标签：视频 / 图文 / 各合集 / 合集与系列。
 *
 * 和直播分区、收藏夹用的是同一个 [TeakSlidingChipRow]：标签仍是分开的胶囊，
 * 选中底色用弹簧滑过去，切换合集时不再「点谁谁瞬间变色」。
 */
internal fun androidx.compose.foundation.lazy.LazyListScope.spaceSubmissionChips(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val seasons = content.snapshot.seasons
    val articles = content.snapshot.articles.items
    val ui = content.ui
    item(key = "submission-chips") {
        // chips 列表稳定才能让 TeakSlidingChipRow 的 remember(chips) 真正命中，
        // 否则每次重组都会重新量一遍所有标签。
        val chips = remember(seasons, articles) {
            buildList {
                add(TeakChip(VideoChipKey, "视频"))
                add(TeakChip(ArticleChipKey, "图文"))
                seasons.forEach { add(TeakChip(seasonChipKey(it.id), it.title)) }
                add(TeakChip(AllCollectionsChipKey, "合集与系列"))
            }
        }
        val selectedKey = when (ui.submissionMode) {
            0 -> VideoChipKey
            1 -> ArticleChipKey
            2 -> ui.selectedCollection?.let { seasonChipKey(it.id) }
            else -> AllCollectionsChipKey
        }
        TeakSlidingChipRow(
            chips = chips,
            selectedKey = selectedKey,
            onSelect = { chip ->
                when (val key = chip.key) {
                    VideoChipKey -> ui.submissionMode = 0
                    ArticleChipKey -> {
                        ui.submissionMode = 1
                        ui.selectedCollection = null
                        if (articles.isEmpty()) actions.onLoadArticles(1)
                    }
                    AllCollectionsChipKey -> {
                        ui.submissionMode = 3
                        ui.selectedCollection = null
                    }
                    else -> seasons.firstOrNull { seasonChipKey(it.id) == key }
                        ?.let { season -> actions.onLoadCollection(season) }
                }
            },
            // 这个 item 铺满整行；收藏夹网格有 12dp 内容边距，这里补同样的起始内边距，
            // 否则第一枚「视频」标签会比收藏夹第一枚标签更贴左。
            modifier = Modifier.padding(start = 12.dp),
            // 再抵消 12dp，让标签能滚到屏幕边缘。
            bleed = 12.dp,
            verticalPadding = 6.dp,
        )
    }
}

private const val VideoChipKey = "submission-video"
private const val ArticleChipKey = "submission-article"
private const val AllCollectionsChipKey = "submission-all-collections"
private fun seasonChipKey(id: Long) = "submission-season-$id"
