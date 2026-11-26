package com.aritxonly.bdfun.ui

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
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
 *
 * 滚动时 `onGloballyPositioned` 每帧都会回调；这里只保存坐标引用，
 * 屏幕矩形在真正打开动画时才换算（不逐帧分配 Rect，也不逐帧调 getLocationOnScreen）。
 */
class OneTakeAnchor internal constructor() {

    internal var coordinates: LayoutCoordinates? = null
    internal var hostView: View? = null
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
        TeakOneTake.openVideo(context, video, screenRect(), radiusPx, color, fallback)
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
        TeakOneTake.openFeed(context, dynamic, screenRect(), radiusPx, color, fallback)
    }

    /** 只在真正起飞时换算一次：布局坐标 → 屏幕矩形。 */
    private fun screenRect(): Rect? {
        val coords = coordinates?.takeIf { it.isAttached } ?: return null
        val view = hostView ?: return null
        val bounds = coords.boundsInRoot()
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return Rect(
            (bounds.left + location[0]).roundToInt(),
            (bounds.top + location[1]).roundToInt(),
            (bounds.right + location[0]).roundToInt(),
            (bounds.bottom + location[1]).roundToInt(),
        )
    }
}

@Composable
fun rememberOneTakeAnchor(): OneTakeAnchor {
    val view = LocalView.current
    val pageBackground = MiuixTheme.colorScheme.background.toArgb()
    val anchor = remember { OneTakeAnchor() }
    anchor.backgroundColor = pageBackground
    anchor.hostView = view
    anchor.modifier = remember(anchor, view) {
        Modifier.onGloballyPositioned { coords -> anchor.coordinates = coords }
    }
    return anchor
}
