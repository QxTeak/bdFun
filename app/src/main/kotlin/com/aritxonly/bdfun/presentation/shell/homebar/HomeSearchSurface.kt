package com.aritxonly.bdfun.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
internal fun androidx.compose.foundation.layout.RowScope.HomeSearchSurface(search: HomeTopBarSearch,
    actions: HomeTopBarActions, material: HomeTopBarMaterial) {
    val wide = material.wide
    val searchWidth = material.searchWidth
    val classic = material.classic
    val searchActive = search.active
    val searchResultsVisible = search.resultsVisible
    val searchInteraction = material.searchInteraction
    val onSearch = actions.onSearch
    val recipe = material.recipe
    val backdrop = material.backdrop
    // 搜索框高度跟返回键玻璃圆面直径一致：返回键多大，胶囊就多高，并排时上下边对齐。
    val searchHeight = com.aritxonly.teak.common.chrome.teakTopBarActionSize()
    // 经典样式：搜索框是平面浅灰胶囊，不叠玻璃高光/阴影。
    if (classic) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .then(if (wide) Modifier.width(searchWidth) else Modifier.weight(1f))
                .height(searchHeight)
                .background(MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f), Capsule())
                .then(
                    if (!searchActive) Modifier.clickable(
                        interactionSource = searchInteraction,
                        indication = null,
                        onClick = onSearch,
                    ) else Modifier,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            HomeSearchContents(search, actions, material)
        }
        return
    }
    TeakGlassSurface(
        modifier = Modifier
            .then(if (wide) Modifier.width(searchWidth) else Modifier.weight(1f))
            .height(searchHeight)
            .then(
                when {
                    !searchActive -> Modifier.clickable(
                        interactionSource = searchInteraction,
                        indication = null,
                        onClick = onSearch,
                    )
                    searchResultsVisible -> Modifier
                    else -> Modifier
                },
            ),
        shape = Capsule(),
        flatBackgroundAlpha = com.aritxonly.teak.common.material.TeakGlassDefaults.flatBackgroundAlpha(),
        recipe = recipe,
        backdrop = backdrop,
        contentAlignment = Alignment.CenterStart,
    ) {
        HomeSearchContents(search, actions, material)
    }

}
