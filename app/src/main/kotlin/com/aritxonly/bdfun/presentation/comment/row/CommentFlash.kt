package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.aritxonly.teak.common.material.teakFlashIndication

/**
 * 一次评论定位/发送的高亮目标。
 *
 * [seq] 每次触发都会变化，保证同一条评论被重复定位时也能再闪一次；
 * 主评论、折叠回复、楼中楼回复共用这一份状态，不再各写一套高亮逻辑。
 */
internal data class CommentFlashTarget(val rpid: Long, val seq: Int)

internal val LocalCommentFlashTarget = staticCompositionLocalOf<CommentFlashTarget?> { null }

/** 只有被定位到的那一行才挂载按压指示节点，其它行保持零开销。 */
@Composable
internal fun Modifier.commentFlash(rpid: Long, shape: Shape = RectangleShape): Modifier {
    val target = LocalCommentFlashTarget.current
    return teakFlashIndication(
        flashKey = target?.let { flash -> flash.seq.takeIf { it > 0 && flash.rpid == rpid } },
        shape = shape,
    )
}
