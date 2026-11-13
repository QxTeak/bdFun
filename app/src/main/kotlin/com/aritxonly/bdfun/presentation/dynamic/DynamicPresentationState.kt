package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*

internal class DynamicPullStateHolder { var pull by mutableFloatStateOf(0f) }
internal class DynamicRefreshRequestStateHolder(tick: Int) { var handledTick by mutableIntStateOf(tick) }
internal class DynamicAutoPlayStateHolder { var autoPlayId by mutableStateOf<String?>(null) }
internal data class DynamicFeedMedia(
    val autoPlay: Boolean, val autoPlayId: String?, val muted: Boolean, val onToggleMute: () -> Unit,
)
