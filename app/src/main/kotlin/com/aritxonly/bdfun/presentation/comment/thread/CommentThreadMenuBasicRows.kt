package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.core.model.comment.CommentItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommentThreadMenuBasicRows(runtime: CommentThreadRuntime, input: CommentThreadInput, menuTarget: CommentItem) {
    val colors = MiuixTheme.colorScheme
    top.yukonga.miuix.kmp.glass.GlassPopupItem(
        text = "回复",
        contentColor = colors.onBackground,
        onClick = { runtime.dismissMenu { runtime.transient.replyTarget = menuTarget } },
    )
    top.yukonga.miuix.kmp.glass.GlassPopupItem(
        text = "复制",
        contentColor = colors.onBackground,
        onClick = {
            runtime.context.copyToClipboard("评论", menuTarget.message)
            runtime.context.showToast("已复制")
            runtime.dismissMenu()
        },
    )
    top.yukonga.miuix.kmp.glass.GlassPopupItem(
        text = "自由复制",
        contentColor = colors.onBackground,
        onClick = {
            runtime.context.copyToClipboard("评论", "${menuTarget.uname}：${menuTarget.message}")
            runtime.context.showToast("已复制")
            runtime.dismissMenu()
        },
    )
    top.yukonga.miuix.kmp.glass.GlassPopupItem(
        text = "分享",
        contentColor = colors.onBackground,
        onClick = {
            runCatching {
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                    .apply {
                        type = "text/plain"
                        putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            "${menuTarget.uname}：${menuTarget.message}\n" +
                                "https://www.bilibili.com/video/${input.bvid}",
                        )
                    }
                runtime.context.startActivity(
                    android.content.Intent.createChooser(intent, "分享评论"),
                )
            }
            runtime.dismissMenu()
        },
    )
}
