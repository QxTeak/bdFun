package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.model.video.VideoItem

internal class HomePresentationStateHolder {
    var actionVideo by mutableStateOf<VideoItem?>(null)
    var pullOffset by mutableFloatStateOf(0f)
}
