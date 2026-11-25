package com.aritxonly.bdfun

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
    val chips = buildList {
        add(TeakChip(VideoChipKey, "视频"))
        add(TeakChip(ArticleChipKey, "图文"))
        seasons.forEach { add(TeakChip(seasonChipKey(it.id), it.title)) }
        add(TeakChip(AllCollectionsChipKey, "合集与系列"))
    }
    val selectedKey = when (ui.submissionMode) {
        0 -> VideoChipKey
        1 -> ArticleChipKey
        2 -> ui.selectedCollection?.let { seasonChipKey(it.id) }
        else -> AllCollectionsChipKey
    }
    item(key = "submission-chips") {
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
            // 这个 item 铺满整行，抵消 12dp 后标签和页面其它内容对齐，并能滚到屏幕边缘。
            bleed = 12.dp,
            verticalPadding = 6.dp,
        )
    }
}

private const val VideoChipKey = "submission-video"
private const val ArticleChipKey = "submission-article"
private const val AllCollectionsChipKey = "submission-all-collections"
private fun seasonChipKey(id: Long) = "submission-season-$id"
