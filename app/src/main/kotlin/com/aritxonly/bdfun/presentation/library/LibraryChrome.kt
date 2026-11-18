package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.library.application.*
import com.aritxonly.teak.common.chrome.TeakTopGlassButton
import com.aritxonly.teak.common.chrome.teakGlassGlyphColor
import com.aritxonly.teak.common.navigation.TeakBatchAction
import com.aritxonly.teak.common.navigation.TeakBatchActionBar
import com.aritxonly.teak.common.navigation.TeakMorphMenuAnchor
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Search

@Composable
fun LibraryTopActions(menuState: TeakMorphMenuState) {
    val LibraryPageBus = LocalPageChromeState.current

    TeakTopGlassButton(onClick = { LibraryPageBus.requestSearch() }) {
        LibrarySearchGlyph()
    }
    // 省略号现在走 BiliPai 同款形变菜单：按钮被吸进玻璃，菜单从按钮位置长出来。
    TeakMorphMenuAnchor(menuState) { onClick ->
        TeakTopGlassButton(onClick = onClick) {
            LibraryThreeDotsGlyph()
        }
    }
}

@Composable
fun LibraryMorphMenuContent(dismiss: (after: () -> Unit) -> Unit) {
    val LibraryPageBus = LocalPageChromeState.current

    GlassPopupItem(
        text = "回到顶部",
        onClick = { dismiss { LibraryPageBus.requestScrollTop() } },
    )
    GlassPopupItem(
        text = "批量管理",
        onClick = { dismiss { LibraryPageBus.requestBatch() } },
    )
}

@Composable
fun LibraryBatchOverlay() {
    val LibraryPageBus = LocalPageChromeState.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        TeakBatchActionBar(
            visible = LibraryPageBus.batchVisible,
            selectedCount = LibraryPageBus.batchCount,
            actions = buildList {
                add(TeakBatchAction("all", "全选"))
                add(
                    TeakBatchAction(
                        "delete",
                        "删除",
                        enabled = LibraryPageBus.batchCount > 0,
                        danger = true,
                    ),
                )
                if (LibraryPageBus.batchCanTransfer) {
                    add(
                        TeakBatchAction(
                            "move",
                            "移动到",
                            enabled = LibraryPageBus.batchCount > 0,
                        ),
                    )
                    add(
                        TeakBatchAction(
                            "copy",
                            "复制到",
                            enabled = LibraryPageBus.batchCount > 0,
                        ),
                    )
                }
            },
            onAction = { LibraryPageBus.sendBatchAction(it.key) },
            modifier = Modifier
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 14.dp),
        )
    }
}

@Composable
internal fun LibrarySearchGlyph() {
    top.yukonga.miuix.kmp.basic.Icon(
        imageVector = top.yukonga.miuix.kmp.icon.MiuixIcons.Search,
        contentDescription = "搜索",
        tint = teakGlassGlyphColor(),
        modifier = Modifier.size(22.dp),
    )
}

@Composable
internal fun LibraryThreeDotsGlyph() {
    top.yukonga.miuix.kmp.basic.Icon(
        imageVector = top.yukonga.miuix.kmp.icon.MiuixIcons.More,
        contentDescription = "更多",
        tint = teakGlassGlyphColor(),
        modifier = Modifier.size(22.dp),
    )
}
