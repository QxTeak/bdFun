package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*

internal class FeedPullStateHolder { var pull by mutableFloatStateOf(0f) }
internal class FeedAutoPlayStateHolder {
    private val selectedId = mutableStateOf<String?>(null)
    val autoPlayId: State<String?> = selectedId

    fun select(id: String?) {
        selectedId.value = id
    }
}

internal data class FeedMedia(
    val autoPlay: Boolean, val autoPlayId: State<String?>, val muted: Boolean, val onToggleMute: () -> Unit,
)
