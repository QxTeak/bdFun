package com.aritxonly.bdfun.ui

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect

internal class DynamicImageBoundsStateHolder { val bounds = mutableStateMapOf<String, Rect>() }
