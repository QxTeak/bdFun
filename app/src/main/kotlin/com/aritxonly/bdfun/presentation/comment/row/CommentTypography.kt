package com.aritxonly.bdfun.ui

import androidx.compose.ui.unit.sp

/** 主评论、折叠回复及展开楼中楼使用相同正文度量。 */
internal object CommentTypography {
    val bodySize = 14.85.sp
    val bodyLineHeight = 21.8.sp

    /** 折叠回复：正文比主评论小 1sp，用户名比折叠正文大 1sp（即回到主评论字号）。 */
    val previewBodySize = bodySize - 1.sp
    val previewLineHeight = bodyLineHeight - 1.sp
    val previewNameSize = previewBodySize + 1.sp
}
