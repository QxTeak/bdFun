package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.aritxonly.bdfun.core.model.comment.CommentItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun CommentThreadMenuModerationRows(runtime: CommentThreadRuntime, menuTarget: CommentItem) {
    val myMid = LocalVideoPageState.current.dependencies.comments.session.value?.mid ?: 0L
    top.yukonga.miuix.kmp.glass.GlassPopupItem(
        text = "举报",
        contentColor = Color(0xFFE5484D),
        onClick = {
            runtime.transient.menuVisible = false
            runtime.scope.launch {
                val ok = runtime.actions.report(menuTarget.rpid)
                if (!runtime.controller.isActive) return@launch
                runtime.context.showToast(if (ok) "已举报" else "举报失败")
                delay(360)
                runtime.transient.menuReply = null
            }
        },
    )
    top.yukonga.miuix.kmp.glass.GlassPopupItem(
        text = "加入黑名单",
        contentColor = Color(0xFFE5484D),
        onClick = {
            runtime.transient.menuVisible = false
            runtime.scope.launch {
                val ok = runtime.actions.block(menuTarget.mid)
                if (!runtime.controller.isActive) return@launch
                runtime.context.showToast(if (ok) "已加入黑名单" else "操作失败")
                delay(360)
                runtime.transient.menuReply = null
            }
        },
    )
    if (myMid > 0 && menuTarget.mid == myMid) {
        top.yukonga.miuix.kmp.glass.GlassPopupItem(
            text = "删除",
            contentColor = Color(0xFFE5484D),
            onClick = {
                runtime.transient.menuVisible = false
                runtime.scope.launch {
                    val ok = runtime.controller.delete(menuTarget.rpid)
                    if (!runtime.controller.isActive) return@launch
                    if (ok) {
                        runtime.context.showToast("已删除")
                    } else {
                        runtime.context.showToast("删除失败")
                    }
                    delay(360)
                    runtime.transient.menuReply = null
                }
            },
        )
    }
}
