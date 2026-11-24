package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 输入框统一字体：去掉字体上下不对称的 padding，光标才能真的在垂直中心。 */
val ComposerTextStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.Normal,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)
