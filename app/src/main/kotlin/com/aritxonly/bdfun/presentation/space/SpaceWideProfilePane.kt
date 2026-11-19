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
    val mid = content.mid
    val isSelf = content.isSelf
    val profile = content.snapshot.profile.space?.profile
    val followed = content.snapshot.profile.followed
    val followBusy = content.snapshot.profile.followBusy
    val banner = content.banner
    val profileWidth = layout.profileWidth
    val profileListState = layout.profileListState
    LazyColumn(
        state = profileListState,
        modifier = Modifier.width(profileWidth).fillMaxHeight(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) { item(key = "profile") {
        WideSpaceProfile(profile, banner.bitmap, mid, isSelf, followed, followBusy) { actions.onFollow() }
    } }
}
