package com.aritxonly.bdfun

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.ui.showToast

internal fun androidx.compose.foundation.lazy.LazyListScope.spaceShopItems(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val context = actions.context
    val shop = content.snapshot.shop
    val items = shop.items
    val tabEnterModifier = Modifier
    // 小店是独立的商品列表，之前误用了投稿视频的 items/空态文案。
    // 保持两列，但把卡片做矮做紧凑；外层补 12dp，不能贴屏幕边缘。
    val rows = items.chunked(2)
    itemsIndexed(rows, key = { index, row -> "shop-$index-" + row.first().url.ifBlank { row.first().title } }) { _, row ->
        Row(
            // 行与行之间也要留缝，不然商品卡会上下贴在一起。
            modifier = tabEnterModifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            row.forEach { item ->
                SpaceShopCard(
                    item = item,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (item.url.isBlank()) {
                            context.showToast("该商品暂未提供打开链接")
                        } else {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(item.url)),
                                )
                            }.onFailure { context.showToast("无法打开商品") }
                        }
                    },
                )
            }
            repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    if (shop.loading) {
        item(key = "shop-loading") { SpaceLoadingRow() }
    } else if (items.isEmpty()) {
        item(key = "shop-empty") { SpaceEmpty("小店里还没有商品") }
    }
}
