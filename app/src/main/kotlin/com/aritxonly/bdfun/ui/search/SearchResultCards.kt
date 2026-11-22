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
        // 澶栧湀 52dp銆佸ご鍍?48dp锛氬渾褰㈣鍒囦笌鎻忚竟閮界暀鍑?2dp 浣欓噺锛屽ご鍍忎笉浼氳鍒囨帀涓€鍦堛€?
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = rememberImageRequest(user.face),
                contentDescription = user.name,
                // 鎼滅储鐢ㄦ埛澶村儚鎸夊畬鏁村浘鐗囨樉绀猴紱Crop 浼氭妸闈?1:1 澶村儚鐨勪笂涓?宸﹀彸鍒囨帀銆?
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
    /** 鏀惰棌/鍘嗗彶/绋嶅悗鍐嶇湅澶嶇敤杩欎竴琛屾椂锛岄暱鎸夊脊鍚屾鎿嶄綔闈㈡澘銆?*/
    onLongClick: (() -> Unit)? = null,
    /** 闀挎寜/鐪佺暐鍙峰叡鐢細浼犲叆鏁磋绐楀彛鍧愭爣锛屼笅鎷夎彍鍗曟嵁姝ゅ畾浣嶃€?*/
    onLongPressRect: ((Rect) -> Unit)? = null,
    onMoreClick: ((Rect) -> Unit)? = null,
    /** 鎵归噺绠＄悊锛歯ull 琛ㄧず涓嶆樉绀哄嬀閫夛紝true/false 琛ㄧず宸查€?鏈€夈€?*/
    selection: Boolean? = null,
) {
    val coverWidth = 150.dp
    val coverHeight = coverWidth * 9f / 16f
    // 鑿滃崟瑕佸脊鍦ㄦ墜鎸囨寜涓嬬殑浣嶇疆锛堝拰璇勮鍖轰竴鑷达級锛宑ombinedClickable 涓嶇粰鍧愭爣锛?
    // 杩欓噷鏃佸惉鎸囬拡浣嶇疆锛沚ounds 鍚屾椂鎹㈢畻鍒板睆骞曞潗鏍囷紝寮圭獥閿氱偣鎵嶄笉浼氬亸鐘舵€佹爮銆?
    val bounds = remember { CardBoundsHolder() }
    // 指针位置每帧都会变，用普通数组记录，避免滚动/滑动时逐帧写快照状态。
    val pressPosition = remember { FloatArray(2) }
    val view = LocalView.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val location = IntArray(2).also { view.getLocationOnScreen(it) }
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
            .then(
                if (onLongClick == null && onLongPressRect == null) {
                    Modifier.clickable(interactionSource = null, indication = null, onClick = onClick)
                } else {
                    Modifier.combinedClickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onClick,
                        onLongClick = {
                            onLongPressRect?.invoke(
                                Rect(
                                    bounds.value.left + pressPosition[0],
                                    bounds.value.top + pressPosition[1],
                                    bounds.value.left + pressPosition[0],
                                    bounds.value.top + pressPosition[1],
                                ),
                            ) ?: onLongClick?.invoke()
                        },
                    )
                },
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        SearchVideoCover(video, coverWidth, coverHeight)
        Spacer(Modifier.width(10.dp))
        // 楂樺害鍙粰涓嬮檺锛氬瓧鍙锋斁澶с€佷袱琛屾爣棰樻椂鍙冲垪鍙拺楂橈紝UP 琛屼笉浼氳鎸ゅ嚭瀹瑰櫒瑁佹帀銆?
        SearchResultInfo(video, Modifier.weight(1f).heightIn(min = coverHeight))
        if (onMoreClick != null) {
            // 鍜岃瘎璁鸿灏惧悓鏍风殑 Miuix More 鍥炬爣锛屾寕鍦ㄦ暣琛屽彸涓嬭銆?
            VideoCardMoreButton(
                onClick = { onMoreClick(bounds.value) },
                modifier = Modifier.align(Alignment.Bottom).padding(start = 2.dp),
            )
        }
        // 鎵归噺绠＄悊鍕鹃€夌洿鎺ュ鐢?Miuix 鐨?Checkbox锛堝拰閫夋敹钘忓す鐨勯潰鏉垮悓娆撅級銆?
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

/** 鏍囬 + 鎾斁/寮瑰箷 + UP锛氬彸鍒楅珮搴︿笉浣庝簬灏侀潰锛屾爣棰樹笌鏁版嵁璐撮《銆乁P 璐村簳銆?*/
@Composable
private fun SearchResultInfo(video: VideoItem, modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    // 鍙粰涓嬮檺鑰屼笉鏄啓姝婚珮搴︼細瀛楀彿鏀惧ぇ鎴栦袱琛屾爣棰樻椂鍙冲垪鑳借嚜宸辨拺楂橈紝
    // 鍓╀綑绌虹櫧浜ょ粰 SpaceBetween 鍏ㄨ惤鍦ㄦ暟鎹涓?UP 琛屼箣闂达紝閬垮厤 UP 琛岃鎸ゅ嚭瀹瑰櫒瑁佹帀銆?
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
                if (video.pubdate > 0L) {
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

@Composable
private fun SearchVideoCover(video: VideoItem, coverWidth: androidx.compose.ui.unit.Dp, coverHeight: androidx.compose.ui.unit.Dp) {
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
