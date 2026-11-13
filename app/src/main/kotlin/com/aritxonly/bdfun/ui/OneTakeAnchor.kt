package com.aritxonly.bdfun.ui

import android.content.Context
import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import com.aritxonly.bdfun.TeakOneTake
import com.aritxonly.bdfun.data.VideoItem
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 一镜到底的「起飞点」。
 *
 * 用法：
 * ```
 * val oneTake = rememberOneTakeAnchor()
 * VideoGridCard(
 *     modifier = oneTake.modifier,
 *     onClick = { oneTake.open(context, video, radiusPx) { 普通跳转() } },
 * )
 * ```
 * 它自己记着卡片在屏幕上的矩形，点的时候交给系统当放大起点。
 */
class OneTakeAnchor internal constructor() {

    internal var rect: Rect? = null
    internal var modifier: Modifier = Modifier

    /** 系统在缩放过程中铺在底下的颜色；给黑色会看到整屏发黑，所以默认用页面底色。 */
    internal var backgroundColor: Int = 0

    /**
     * @param radiusPx 卡片圆角（px），要和卡片看起来一致，否则起飞瞬间会跳一下
     * @param backgroundColor 系统在图片背后铺的底色，不传就用当前页面背景色
     */
    fun open(
        context: Context,
        video: VideoItem,
        radiusPx: Float,
        backgroundColor: Int = 0,
        fallback: () -> Unit,
    ) {
        val color = if (backgroundColor != 0) backgroundColor else this.backgroundColor
        TeakOneTake.openVideo(context, video, rect, radiusPx, color, fallback)
    }

    /** 从整张动态卡片放大到动态详情。 */
    fun openFeed(
        context: Context,
        dynamic: com.aritxonly.bdfun.data.FeedItem,
        radiusPx: Float,
        backgroundColor: Int = 0,
        fallback: () -> Unit,
    ) {
        val color = if (backgroundColor != 0) backgroundColor else this.backgroundColor
        TeakOneTake.openFeed(context, dynamic, rect, radiusPx, color, fallback)
    }
}

@Composable
fun rememberOneTakeAnchor(): OneTakeAnchor {
    val view = LocalView.current
    val pageBackground = MiuixTheme.colorScheme.background.toArgb()
    val anchor = remember { OneTakeAnchor() }
    anchor.backgroundColor = pageBackground
    anchor.modifier = remember(anchor, view) {
        Modifier.onGloballyPositioned { coords ->
            val b = coords.boundsInRoot()
            val loc = IntArray(2)
            view.getLocationOnScreen(loc)
            anchor.rect = Rect(
                (b.left + loc[0]).roundToInt(),
                (b.top + loc[1]).roundToInt(),
                (b.right + loc[0]).roundToInt(),
                (b.bottom + loc[1]).roundToInt(),
            )
        }
    }
    return anchor
}
