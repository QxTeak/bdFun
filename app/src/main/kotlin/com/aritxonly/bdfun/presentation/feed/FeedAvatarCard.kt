package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.theme.TeakUiStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 普通动态页的头像卡片：圆角矩形底 + 横向头像行。
 *
 * 行本体的实现已经抽到 [FeedAvatarRow]：UP 页把它直接当顶栏用（不要卡片底、不要直播头像），
 * 两边共用同一套环、滚动和「更多 UP」分页，不复制第二份。
 */
@Composable
internal fun FeedAvatarCard(
    state: FeedState,
    ring: FeedAvatarRingState,
    modifier: Modifier = Modifier,
    /** UP 之间切换：由页面统一跑「旧页滑出、新页滑入」，不要在卡片里直接 filterTo。 */
    onSelectUp: (Long) -> Unit,
    /** 点「全部动态」退出 UP 模式：页面自己决定淡出，卡片不直接 clearFilter。 */
    onExitUp: () -> Unit,
    /**
     * 环跟随的选中项。UP 模式下它比 [FeedState.filterMid] 更早变化：
     * pager 一开始翻页环就走，数据等停稳再换。
     */
    selectedMid: Long? = state.filterMid,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier.fillMaxWidth()
            .clip(TeakUiStyle.design.cardShape(ContinuousCornerShape(18.dp)))
            .background(colors.surfaceContainer),
    ) {
        FeedAvatarRow(
            state = state,
            ring = ring,
            selectedMid = selectedMid,
            onSelectUp = onSelectUp,
            onExitUp = onExitUp,
            includeLive = true,
            contentPadding = PaddingValues(14.dp),
            // 动态页的环固定在「全部动态」上：点 UP 是直接开新页，环不跟着走。
            ringAnimated = false,
        )
    }
}
