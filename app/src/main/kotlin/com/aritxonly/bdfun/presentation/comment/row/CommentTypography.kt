package com.aritxonly.bdfun.ui

import androidx.compose.ui.unit.sp

/** 主评论、折叠回复及展开楼中楼使用相同正文度量。 */
internal object CommentTypography {
    val bodySize = 14.85.sp
    val bodyLineHeight = 21.8.sp

    /** 折叠回复：正文比主评论小 1sp；用户名和后面的正文同样大小（不再放大 1sp）。 */
    val previewBodySize = (bodySize.value - 1).sp
    val previewLineHeight = (bodyLineHeight.value - 1).sp
    val previewNameSize = previewBodySize
}
