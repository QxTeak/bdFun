package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.aritxonly.bdfun.BrandPink

internal fun inlinePlayerAppearance() = InlinePlayerAppearance(
    TeakIcon.PlayerPlay, TeakIcon.Fullscreen, BrandPink, TeakIcon.PlayerNext,
)

@Composable
internal fun InlinePlayerCover(cover: String, modifier: Modifier) {
    AsyncImage(model = rememberImageRequest(cover), contentDescription = null,
        contentScale = ContentScale.Crop, modifier = modifier)
}
