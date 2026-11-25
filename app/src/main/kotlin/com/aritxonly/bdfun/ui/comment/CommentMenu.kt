package com.aritxonly.bdfun.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.aritxonly.bdfun.core.model.comment.CommentItem
import com.aritxonly.bdfun.feature.comment.application.CommentActionsStateHolder
import com.aritxonly.teak.common.navigation.LocalTeakMorphMenu
import com.aritxonly.teak.common.navigation.TeakMorphMenuItem
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 评论长按 / 行尾省略号的操作菜单。
 *
 * 和首页视频卡片、收藏页共用 Activity 根部的玻璃形变菜单：省略号从图标展开，
 * 长按从手指位置展开。这里只保存当前目标与菜单项。
 */
internal class CommentMenuStateHolder(
    private val context: Context, private val scope: CoroutineScope,
    private val actions: CommentActionsStateHolder, private val bvid: String,
    private val onReply: (CommentItem) -> Unit, private val onDelete: (Long) -> Unit,
) {
    var target by mutableStateOf<CommentItem?>(null)
        private set
    var morphState by mutableStateOf<TeakMorphMenuState?>(null)
        private set
    var point by mutableStateOf<Offset?>(null)
        private set

    /** 每次打开加一：同一个锚点重复点击也要重新播入场动画。 */
    var requestId by mutableIntStateOf(0)
        private set
    /** 自由复制弹层当前展示的评论。 */
    var copySheet by mutableStateOf<CommentItem?>(null)
        private set

    fun open(item: CommentItem, state: TeakMorphMenuState, point: Offset?) {
        if (!actions.isActive) return
        this.target = item
        morphState = state
        this.point = point
        requestId += 1
    }

    fun hide() {
        target = null
        morphState = null
        point = null
    }

    fun dismiss() = hide()

    fun closeCopySheet() { copySheet = null }
    fun reply(item: CommentItem) {
        if (!actions.isActive) return
        hide()
        onReply(item)
    }
    fun copy(item: CommentItem, withName: Boolean) {
        if (!actions.isActive) return
        hide()
        context.copyToClipboard("评论", if (withName) "${item.uname}：${item.message}" else item.message)
        context.showToast("已复制")
    }
    /** 自由复制：弹层里展示原文，文本可以按住自由拖选。 */
    fun freeCopy(item: CommentItem) {
        if (!actions.isActive) return
        hide()
        copySheet = item
    }
    /** 评论附图整批存相册。 */
    fun savePictures(item: CommentItem) {
        if (!actions.isActive || item.pictures.isEmpty()) return
        hide()
        scope.launch {
            val (saved, total) = ImageSaver.save(context, item.pictures)
            if (!actions.isActive) return@launch
            context.showToast(
                when {
                    saved == total && total > 0 -> "已保存 $saved 张图片"
                    saved > 0 -> "已保存 $saved/$total 张，部分失败"
                    else -> "保存失败"
                },
            )
        }
    }
    fun share(item: CommentItem) {
        if (!actions.isActive) return
        hide()
        runCatching {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, "${item.uname}：${item.message}\nhttps://www.bilibili.com/video/$bvid")
            }
            context.startActivity(android.content.Intent.createChooser(intent, "分享评论"))
        }
    }
    fun report(item: CommentItem) {
        if (!actions.isActive) return
        hide()
        scope.launch {
            val ok = actions.report(item.rpid)
            if (!actions.isActive) return@launch
            context.showToast(if (ok) "已举报" else "举报失败")
        }
    }
    fun block(item: CommentItem) {
        if (!actions.isActive) return
        hide()
        scope.launch {
            val ok = actions.block(item.mid)
            if (!actions.isActive) return@launch
            context.showToast(if (ok) "已加入黑名单" else "操作失败")
        }
    }
    fun delete(item: CommentItem) {
        if (!actions.isActive) return
        hide()
        scope.launch {
            val ok = actions.delete(item.rpid)
            if (!actions.isActive) return@launch
            if (ok) { onDelete(item.rpid); context.showToast("已删除") } else context.showToast("删除失败")
        }
    }
}

@Composable
internal fun CommentMenu(menu: CommentMenuStateHolder, myMid: Long) {
    val controller = LocalTeakMorphMenu.current
    val target = menu.target
    val state = menu.morphState
    val point = menu.point
    LaunchedEffect(menu.requestId) {
        val item = target ?: return@LaunchedEffect
        val anchor = state ?: return@LaunchedEffect
        val morph = controller ?: return@LaunchedEffect
        morph.show(
            state = anchor,
            items = commentMorphItems(menu, item, myMid),
            onDismiss = menu::hide,
            showAnchorCopy = point == null,
            point = point,
        )
    }
    // 自由复制弹层：独立的可选择文本窗口。
    CommentCopySheet(menu.copySheet, menu::closeCopySheet)
}

private fun commentMorphItems(
    menu: CommentMenuStateHolder, target: CommentItem, myMid: Long,
): List<TeakMorphMenuItem> = buildList {
    add(TeakMorphMenuItem("回复") { menu.reply(target) })
    add(TeakMorphMenuItem("复制") { menu.copy(target, false) })
    add(TeakMorphMenuItem("自由复制") { menu.freeCopy(target) })
    add(TeakMorphMenuItem("分享") { menu.share(target) })
    if (target.pictures.isNotEmpty()) {
        add(TeakMorphMenuItem("保存全部图片") { menu.savePictures(target) })
    }
    add(TeakMorphMenuItem("举报", danger = true) { menu.report(target) })
    add(TeakMorphMenuItem("加入黑名单", danger = true) { menu.block(target) })
    if (myMid > 0 && target.mid == myMid) {
        add(TeakMorphMenuItem("删除", danger = true) { menu.delete(target) })
    }
}
