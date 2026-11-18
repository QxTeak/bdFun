package com.aritxonly.teak.common.settings

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.aritxonly.teak.common.theme.TeakDesign

/** 经典分组原本不裁剪子内容，不能用矩形 clip 替代。 */
fun Modifier.teakSectionSurface(design: TeakDesign, modernShape: Shape, color: Color): Modifier =
    when (design) {
        TeakDesign.Classic -> background(color)
        TeakDesign.Modern -> clip(modernShape).background(color)
    }
