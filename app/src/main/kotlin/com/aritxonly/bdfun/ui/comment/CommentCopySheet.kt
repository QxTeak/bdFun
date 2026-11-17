package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.comment.CommentItem
import com.aritxonly.teak.common.modal.MiuixBottomSheet
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 「自由复制」弹层。
 *
 * 与收藏夹选择使用同一个 MiuixBottomSheet 外壳（同样的标题、关闭按钮、底部主按钮），
 * 内容仍是可选择文本：用户能按住任意一段自由拖选，也可以点底部按钮整条复制。
 * 关闭时保留最后一份内容，让 sheet 的退场动画完整播放。
 */
@Composable
internal fun CommentCopySheet(item: CommentItem?, onDismiss: () -> Unit) {
    var displayed by remember { mutableStateOf<CommentItem?>(null) }
    if (item != null) displayed = item
    val content = displayed ?: return
    val context = LocalContext.current
    MiuixBottomSheet(
        show = item != null,
        title = "自由复制",
        onDismissRequest = onDismiss,
        startAction = {
            IconButton(onClick = onDismiss) { Icon(MiuixIcons.Close, "关闭") }
        },
        bottomBar = {
            Button(
                onClick = {
                    context.copyToClipboard("评论", content.message)
                    context.showToast("已复制")
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                cornerRadius = 20.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text("复制全部", color = MiuixTheme.colorScheme.onPrimary)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(padding)
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        ) {
            Text(
                text = content.uname,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(8.dp))
            SelectionContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = content.message,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp,
                )
            }
        }
    }
}
