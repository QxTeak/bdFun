package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface VideoPageOverlayHost {
    val replyEditing: Boolean
    var dismissReply: Int
    var danmakuInputClose: Int
}

class VideoPageOverlaySlots(
    val tabs: @Composable BoxScope.() -> Unit,
    val replyBar: @Composable (Modifier) -> Unit,
    val inputBar: @Composable (() -> Unit, Modifier) -> Unit,
)
