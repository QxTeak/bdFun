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
    TeakGlassSurface(
        modifier = Modifier
            .then(if (wide) Modifier.width(searchWidth) else Modifier.weight(1f))
            .height(40.dp)
            .then(
                if (classic) {
                    Modifier.background(
                        MiuixTheme.colorScheme.surfaceContainerHigh,
                        Capsule(),
                    )
                } else {
                    Modifier
                },
            )
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
        materialAlpha = com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current,
        flatBackgroundAlpha = com.aritxonly.teak.common.material.TeakGlassDefaults.flatBackgroundAlpha(),
        recipe = recipe,
        backdrop = if (classic) null else backdrop,
        contentAlignment = Alignment.CenterStart,
    ) {
        HomeSearchContents(search, actions, material)
    }

}
