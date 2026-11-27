package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.data.SearchUser
import com.aritxonly.bdfun.data.VideoItem
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SearchUserRow(user: SearchUser, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 外圈 52dp、头像 48dp：圆形裁剪与描边都留出 2dp 余量，头像不会被切掉一圈。
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = rememberImageRequest(user.face),
                contentDescription = user.name,
                // 搜索用户头像按完整图片显示；Crop 会把非 1:1 头像的上下或左右切掉。
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHigh),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                color = colors.onBackground,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (user.sign.isNotBlank()) {
                Text(
                    text = user.sign,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${formatCount(user.fans)} 粉丝 · ${user.videos} 投稿",
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
                lineHeight = 13.sp,
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun SearchResultRow(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 收藏/历史/稍后再看复用这一行时，长按弹同款操作面板。 */
    onLongClick: (() -> Unit)? = null,
    /** 长按/省略号共用：传入整行窗口坐标，下拉菜单据此定位。 */
    onLongPressRect: ((Rect) -> Unit)? = null,
    onMoreClick: ((Rect) -> Unit)? = null,
    /** 省略号形变菜单：每行独立锚点，点击后由页面决定菜单内容。 */
    morphState: com.aritxonly.teak.common.navigation.TeakMorphMenuState? = null,
    onMorphOpen: (() -> Unit)? = null,
    /** 批量管理：null 表示不显示勾选，true/false 表示已选/未选。 */
    selection: Boolean? = null,
    /** 播放量那一行是否带上发布时间。 */
    showDate: Boolean = true,
    /** 非空时用它顶掉底部的 UP 行（个人主页投稿页显示发布时间用）。 */
    metaLabel: String? = null,
    /** 封面加载完成后回调它的平均色：删除爆炸的碎屑颜色跟着封面走。 */
    onCoverColor: ((Color) -> Unit)? = null,
) {
    val coverWidth = 150.dp
    val coverHeight = coverWidth * 9f / 16f
    val bounds = remember { CardBoundsHolder() }
    val morph = rememberSearchRowMorph(video, morphState, onMorphOpen, onLongPressRect)
    // 指针位置每帧都会变，用普通数组记录；菜单锚点同时换算到屏幕坐标。
    val pressPosition = remember { FloatArray(2) }
    val view = LocalView.current
    val location = remember { IntArray(2) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                view.getLocationOnScreen(location)
                val windowBounds = coordinates.boundsInWindow()
                bounds.value = Rect(
                    windowBounds.left + location[0],
                    windowBounds.top + location[1],
                    windowBounds.right + location[0],
                    windowBounds.bottom + location[1],
                )
            }
            .pointerInput(video.bvid) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.pressed } ?: continue
                        pressPosition[0] = change.position.x
                        pressPosition[1] = change.position.y
                    }
                }
            }
            .searchRowClicks(onClick, onLongClick, morph.longPress, bounds, pressPosition)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        SearchVideoCover(video, coverWidth, coverHeight, onCoverColor)
        Spacer(Modifier.width(10.dp))
        // 高度只给下限：字号放大、两行标题时右列可撑高，UP 行不会被挤出容器裁掉。
        SearchResultInfo(video, showDate, metaLabel, Modifier.weight(1f).heightIn(min = coverHeight))
        if (onMoreClick != null || morph.state != null) {
            // 和评论行末尾同款的 Miuix More 图标，挂在整行右下角。
            VideoCardMoreButton(
                onClick = { onMoreClick?.invoke(bounds.value) },
                modifier = Modifier.align(Alignment.Bottom).padding(start = 2.dp),
                morphState = morph.state,
                onMorphTrigger = morph.open,
                morphAutoOpen = false,
            )
        }
        // 批量管理勾选直接复用 Miuix 的 Checkbox（和选收藏夹的面板同款）。
        if (selection != null) {
            Checkbox(
                state = if (selection) ToggleableState.On else ToggleableState.Off,
                onClick = null,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 8.dp)
                    .clearAndSetSemantics {},
            )
        }
    }
}

/** 标题 + 播放/弹幕 + UP：右列高度不低于封面，标题与数据贴顶、UP 贴底。 */
@Composable
private fun SearchResultInfo(
    video: VideoItem,
    showDate: Boolean = true,
    metaLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    // 只给下限而不是写死高度：字号放大或两行标题时右列能自己撑高，
    // 剩余空白交给 SpaceBetween 全落在数据行与 UP 行之间，避免 UP 行被挤出容器裁掉。
    Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(
                text = video.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetricGlyph(play = true, color = colors.onBackgroundVariant, size = 11.dp)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatCount(video.play),
                    color = colors.onBackgroundVariant,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.width(6.dp))
                MetricGlyph(play = false, color = colors.onBackgroundVariant, size = 11.dp)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatCount(video.danmaku),
                    color = colors.onBackgroundVariant,
                    fontSize = 10.sp,
                )
                // 发布时间跟着播放量行一起排：不额外占一行，UP 行位置保持原样。
                if (showDate && video.pubdate > 0L) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = formatDate(video.pubdate),
                        color = colors.onBackgroundVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        if (!metaLabel.isNullOrBlank()) {
            // 个人主页投稿页：底部换成发布时间，不再显示 UP 名字。
            Text(
                text = metaLabel,
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
                maxLines = 1,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UpBadge()
                Spacer(Modifier.width(5.dp))
                Text(
                    text = video.ownerName,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SearchVideoCover(
    video: VideoItem,
    coverWidth: androidx.compose.ui.unit.Dp,
    coverHeight: androidx.compose.ui.unit.Dp,
    onCoverColor: ((Color) -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .width(coverWidth)
            .height(coverHeight)
            .clip(ContinuousCornerShape(10.dp))
            .background(colors.surfaceContainerHigh),
    ) {
        AsyncImage(
            model = rememberImageRequest(video.cover),
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onSuccess = { state ->
                onCoverColor?.let { report ->
                    // Coil 2 的 Success 只给 Drawable，这里取底下的 Bitmap 抽平均色。
                    val bitmap = (state.result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    if (bitmap != null) coverAverageColor(bitmap)?.let(report)
                }
            },
        )
        if (video.duration > 0) {
            Text(
                text = formatDuration(video.duration),
                color = Color.White,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 5.dp, bottom = 4.dp),
            )
        }
        VideoChargeBadge(video.isCharge)
    }
}

private class SearchRowMorph(
    val state: com.aritxonly.teak.common.navigation.TeakMorphMenuState?,
    val open: (() -> Unit)?,
    val longPress: ((Rect) -> Unit)?,
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun Modifier.searchRowClicks(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    longPress: ((Rect) -> Unit)?,
    bounds: CardBoundsHolder,
    press: FloatArray,
): Modifier = if (onLongClick == null && longPress == null) {
    clickable(interactionSource = null, indication = null, onClick = onClick)
} else {
    combinedClickable(
        interactionSource = null,
        indication = null,
        onClick = onClick,
        onLongClick = {
            longPress?.invoke(
                Rect(
                    bounds.value.left + press[0],
                    bounds.value.top + press[1],
                    bounds.value.left + press[0],
                    bounds.value.top + press[1],
                ),
            ) ?: onLongClick?.invoke()
        },
    )
}

/** 解析这一行的形变锚点：显式接线优先，否则复用页面提供的本地菜单。 */
@Composable
private fun rememberSearchRowMorph(
    video: com.aritxonly.bdfun.data.VideoItem,
    state: com.aritxonly.teak.common.navigation.TeakMorphMenuState?,
    open: (() -> Unit)?,
    longPress: ((Rect) -> Unit)?,
): SearchRowMorph {
    val local = LocalVideoCardMorphMenu.current
    val moreState = state ?: if (local != null) {
        remember(video.bvid) { com.aritxonly.teak.common.navigation.TeakMorphMenuState() }
    } else null
    val pointState = if (local != null && state == null) {
        remember(video.bvid) { com.aritxonly.teak.common.navigation.TeakMorphMenuState() }
    } else null
    val resolvedLongPress = longPress ?: if (local != null && pointState != null) {
        { rect: Rect -> local.show(video, pointState, Offset(rect.left, rect.top)) }
    } else null
    val resolvedOpen = open ?: if (local != null && moreState != null) {
        { local.show(video, moreState, null) }
    } else null
    return SearchRowMorph(moreState, resolvedOpen, resolvedLongPress)
}
