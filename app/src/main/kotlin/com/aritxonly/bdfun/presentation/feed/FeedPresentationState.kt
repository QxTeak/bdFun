package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*

internal class FeedPullStateHolder { var pull by mutableFloatStateOf(0f) }
internal class FeedAutoPlayStateHolder { var autoPlayId by mutableStateOf<String?>(null) }
internal data class FeedMedia(
    val autoPlay: Boolean, val autoPlayId: String?, val muted: Boolean, val onToggleMute: () -> Unit,
)
