package com.aritxonly.bdfun

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SpaceWideProfilePane(
    content: SpaceScreenContent, layout: SpaceScreenLayout, actions: SpaceScreenActions,
) {
    val profileWidth = layout.profileWidth
    val profileListState = layout.profileListState
    LazyColumn(
        state = profileListState,
        modifier = Modifier.width(profileWidth).fillMaxHeight(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) { item(key = "profile") {
        WideSpaceProfile(content, actions)
    } }
}
